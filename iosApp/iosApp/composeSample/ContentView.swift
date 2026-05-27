import ComposeSampleUI
import SwiftUI
import UIKit

struct ContentView: View {
    var body: some View {
        ComposeWidgetSample()
    }
}

private struct ComposeWidgetSample: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
