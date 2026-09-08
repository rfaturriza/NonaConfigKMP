import SwiftUI
import SharedUI

class NativeRemoteConfigViewModel: ObservableObject {
    @Published var isFetching = false
    @Published var fetchStatus = "Initializing..."
    @Published var currentETag: String? = nil
    
    // Remote parameters state
    @Published var freeShippingThreshold: Int64 = 0
    @Published var maxCartItems: Int64 = 0
    @Published var featureCheckout = false
    @Published var featureLiveChat = false
    @Published var featureNewNav = false
    @Published var killswitchPayments = false
    @Published var supportEmail = ""
    @Published var themePaletteJson = ""
    
    private let client = NonaConfigClient.companion.instance
    
    func setup(apiKey: String) {
        // 1. Initialize client
        client.initialize(
            apiKey: apiKey,
            environmentId: "Development",
            baseUrl: "https://demo.nonaconfig.com"
        )
        
        // 2. Set defaults
        let defaults: [String: Any] = [
            "Checkout:FreeShippingThreshold": Int64(1),
            "Checkout:MaxCartItems": Int64(1),
            "Features:Checkout": false,
            "Features:LiveChat": false,
            "Features:NewNavigation": false,
            "Killswitch:Payments": false,
            "Support:Email": "default-dev-support@acme.example",
            "Theme:Palette": "{\"primary\":\"#5b6ef5\",\"surface\":\"#0f1115\",\"accent\":\"#f5a623\",\"radius\":4}"
        ]
        client.setDefaults(defaults: defaults)
        
        updateKeyValues()
        fetchStatus = "Loaded defaults. Checking cloud..."
        
        performFetch()
    }
    
    func updateKeyValues() {
        DispatchQueue.main.async {
            self.freeShippingThreshold = self.client.getLong(key: "Checkout:FreeShippingThreshold")
            self.maxCartItems = self.client.getLong(key: "Checkout:MaxCartItems")
            self.featureCheckout = self.client.getBoolean(key: "Features:Checkout")
            self.featureLiveChat = self.client.getBoolean(key: "Features:LiveChat")
            self.featureNewNav = self.client.getBoolean(key: "Features:NewNavigation")
            self.killswitchPayments = self.client.getBoolean(key: "Killswitch:Payments")
            self.supportEmail = self.client.getString(key: "Support:Email")
            self.themePaletteJson = self.client.getString(key: "Theme:Palette")
            self.currentETag = self.client.lastETag
        }
    }
    
    func performFetch() {
        DispatchQueue.main.async {
            self.isFetching = true
        }
        
        client.fetchAndActivateWithStatus { status in
            DispatchQueue.main.async {
                switch status {
                case .successNewData:
                    self.fetchStatus = "200 OK (Downloaded New Data)"
                case .successNotModified:
                    self.fetchStatus = "304 Not Modified (ETag Matched)"
                case .throttled:
                    self.fetchStatus = "Throttled (Minimum Fetch Interval Active)"
                case .error:
                    self.fetchStatus = "Fetch Error"
                default:
                    self.fetchStatus = "Fetch Completed"
                }
                self.updateKeyValues()
                self.isFetching = false
            }
        }
    }
    
    func forceFetch() {
        client.clearETag()
        performFetch()
    }
}

struct NativeSwiftUIView: View {
    @StateObject private var viewModel = NativeRemoteConfigViewModel()
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Status")) {
                    HStack {
                        Text("Status:")
                        Spacer()
                        Text(viewModel.fetchStatus)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let tag = viewModel.currentETag {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Current ETag:")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text(tag)
                                .font(.system(.caption, design: .monospaced))
                        }
                    }
                    if viewModel.isFetching {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                    }
                }
                
                Section(header: Text("Native Remote Parameters")) {
                    ParameterRow(name: "Checkout:FreeShippingThreshold", value: "\(viewModel.freeShippingThreshold)", type: "Number")
                    ParameterRow(name: "Checkout:MaxCartItems", value: "\(viewModel.maxCartItems)", type: "Number")
                    ParameterRow(name: "Features:Checkout", value: "\(viewModel.featureCheckout)", type: "Boolean")
                    ParameterRow(name: "Features:LiveChat", value: "\(viewModel.featureLiveChat)", type: "Boolean")
                    ParameterRow(name: "Features:NewNavigation", value: "\(viewModel.featureNewNav)", type: "Boolean")
                    ParameterRow(name: "Killswitch:Payments", value: "\(viewModel.killswitchPayments)", type: "Boolean")
                    ParameterRow(name: "Support:Email", value: viewModel.supportEmail, type: "Text")
                    ParameterRow(name: "Theme:Palette", value: viewModel.themePaletteJson, type: "JSON String")
                }
                
                Section {
                    Button(action: {
                        viewModel.performFetch()
                    }) {
                        Label("Fetch (Send ETag)", systemImage: "arrow.clockwise")
                    }
                    .disabled(viewModel.isFetching)
                    
                    Button(action: {
                        viewModel.forceFetch()
                    }) {
                        Label("Force Fetch (Clear ETag)", systemImage: "arrow.triangle.2.circlepath")
                            .foregroundColor(.orange)
                    }
                    .disabled(viewModel.isFetching)
                }
            }
            .navigationTitle("Native SwiftUI Sample")
            .onAppear {
                let apiKey = (Bundle.main.object(forInfoDictionaryKey: "NONA_API_KEY") as? String)
                    .flatMap { $0.isEmpty ? nil : $0 }
                    ?? ProcessInfo.processInfo.environment["NONA_API_KEY"]
                    ?? ""
                viewModel.setup(apiKey: apiKey)
            }
        }
    }
}

private struct ParameterRow: View {
    let name: String
    let value: String
    let type: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(name) (\(type))")
                .font(.caption)
                .foregroundColor(.secondary)
            Text(value)
                .font(.body)
                .fontWeight(.medium)
        }
        .padding(.vertical, 2)
    }
}
