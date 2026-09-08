import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0

    private var apiKey: String {
        (Bundle.main.object(forInfoDictionaryKey: "NONA_API_KEY") as? String)
            .flatMap { $0.isEmpty ? nil : $0 }
            ?? ProcessInfo.processInfo.environment["NONA_API_KEY"]
            ?? ""
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            CMPComposeView(apiKey: apiKey)
                .ignoresSafeArea(.keyboard)
                .tabItem {
                    Label("Compose (sharedUI)", systemImage: "paintbrush")
                }
                .tag(0)

            NativeSwiftUIView()
                .tabItem {
                    Label("Native Swift (sharedLogic)", systemImage: "swift")
                }
                .tag(1)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
