import SwiftUI

public struct TranslatedText: View {
    public typealias Translate = (
        _ text: String,
        _ targetLanguage: String,
        _ completion: @escaping (String?) -> Void
    ) -> Void

    let rawText: String
    let translationRequired: Bool
    let targetLanguage: String?
    let seeTranslationText: String
    let hideTranslationText: String
    let translate: Translate

    @State private var isTranslationVisible = false
    @State private var translatedText: String?
    @State private var isLoading = false

    public init(
        rawText: String,
        translationRequired: Bool = false,
        targetLanguage: String? = nil,
        seeTranslationText: String = "See translation",
        hideTranslationText: String = "Hide translation",
        translate: @escaping Translate
    ) {
        self.rawText = rawText
        self.translationRequired = translationRequired
        self.targetLanguage = targetLanguage
        self.seeTranslationText = seeTranslationText
        self.hideTranslationText = hideTranslationText
        self.translate = translate
    }

    public var body: some View {
        if translationRequired {
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color.secondary.opacity(0.28))
                    .frame(width: 3)

                VStack(alignment: .leading, spacing: 2) {
                    Text(isTranslationVisible ? translatedText ?? rawText : rawText)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button(action: toggleTranslation) {
                        if isLoading {
                            ProgressView()
                                .controlSize(.small)
                        } else {
                            Text(isTranslationVisible ? hideTranslationText : seeTranslationText)
                        }
                    }
                    .buttonStyle(.borderless)
                    .disabled(isLoading)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .onAppear { prefetch() }
        } else {
            Text(rawText)
        }
    }

    private func prefetch() {
        guard translatedText == nil, !isLoading else { return }
        let lang = targetLanguage ?? TranslationSDK.companion.instance.defaultLanguage
        isLoading = true
        translate(rawText, lang) { result in
            DispatchQueue.main.async {
                self.translatedText = result
                self.isLoading = false
            }
        }
    }

    private func toggleTranslation() {
        isTranslationVisible.toggle()
    }
}
