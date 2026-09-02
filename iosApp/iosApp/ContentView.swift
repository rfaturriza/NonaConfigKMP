import SwiftUI
import SharedUI

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // You should provide your API Key here, e.g. from a .xcconfig or Environment variable
        let apiKey = ProcessInfo.processInfo.environment["NONA_API_KEY"] ?? "dummy-key"
        return MainViewControllerKt.MainViewController(apiKey: apiKey)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}