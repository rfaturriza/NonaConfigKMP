import SwiftUI
import SharedUI

struct CMPComposeView: UIViewControllerRepresentable {
    let apiKey: String

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(apiKey: apiKey)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
