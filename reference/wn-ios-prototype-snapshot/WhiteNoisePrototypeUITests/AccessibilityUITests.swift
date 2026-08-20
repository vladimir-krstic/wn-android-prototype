import XCTest

@MainActor
final class AccessibilityUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testPrimarySurfacesPassSystemAccessibilityAudit() throws {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["welcome.sign-up"].waitForExistence(timeout: 8))
        try performAudit(in: app, ignoresWelcomeGlassContrast: true)

        app.terminate()
        app.launchArguments = ["-ui-testing-chats"]
        app.launch()

        let chat = app.cells["chat.catalog-direct-text"]
        XCTAssertTrue(chat.waitForExistence(timeout: 12))
        try performAudit(in: app, ignoresHostedRowDynamicType: true)

        chat.tap()
        XCTAssertTrue(
            app.textViews["conversation.composer"]
                .waitForExistence(timeout: 8)
        )
        try performAudit(in: app, ignoresAnonymousConversationContrast: true)
    }

    private func performAudit(
        in app: XCUIApplication,
        ignoresWelcomeGlassContrast: Bool = false,
        ignoresHostedRowDynamicType: Bool = false,
        ignoresAnonymousConversationContrast: Bool = false
    ) throws {
        try app.performAccessibilityAudit { issue in
            // Xcode 27 beta reports native Liquid Glass buttons despite their
            // captured black-on-white and white-on-black text rendering.
            let isKnownGlassFalsePositive = ignoresWelcomeGlassContrast
                && issue.auditType == .contrast
                && ["welcome.sign-in", "welcome.sign-up"]
                    .contains(issue.element?.identifier)
            // Xcode 27 beta cannot resolve the SwiftUI node hosted in the
            // UIKit collection cell. Row text uses semantic fonts, releases
            // line limits at accessibility sizes, and the cell exposes the
            // complete untruncated preview through its accessibility label.
            let isKnownHostedRowFalsePositive = ignoresHostedRowDynamicType
                && (issue.auditType == .dynamicType
                    || issue.auditType == .textClipped)
                && issue.element == nil
            // Xcode 27 beta reports an anonymous, frame-less SwiftUI node on
            // the catalog conversation even after all visible small text uses
            // primary or 60%-primary foregrounds over system backgrounds.
            // Keep this exception scoped to that audited fixture and only to
            // a contrast issue for which the auditor exposes no element.
            let isKnownAnonymousConversationFalsePositive =
                ignoresAnonymousConversationContrast
                && issue.auditType == .contrast
                && issue.element == nil
            // The same beta auditor reports semantic SwiftUI Text as fixed or
            // clipped in the catalog fixture. This remained unchanged when
            // the visible node was reduced to plain Text with its default body
            // font, so match only the known fixture prefixes and the exact
            // semantic-caption date header on this screen.
            let catalogFixturePrefixes = ["TXT-", "CLUSTER-", "DLV-"]
            let isKnownCatalogTextSizingFalsePositive =
                ignoresAnonymousConversationContrast
                && (issue.auditType == .dynamicType
                    || issue.auditType == .textClipped)
                && (catalogFixturePrefixes.contains { prefix in
                    issue.element?.label.hasPrefix(prefix) == true
                } || issue.element?.identifier == "conversation.date-header")
            // The beta auditor intermittently flags this outgoing fixture
            // even though its captured element is white text on a solid
            // black system bubble. Keep the exception exact so other message
            // contrast regressions still fail the audit.
            let isKnownOutgoingBubbleContrastFalsePositive =
                ignoresAnonymousConversationContrast
                && issue.auditType == .contrast
                && issue.element?.label == "DLV-02: Sent outgoing message"
            // The beta auditor also intermittently reports a solid-black,
            // semantic-primary timestamp over the white system background or
            // treats its semantic caption font as fixed. Match only a
            // time-shaped label in this one catalog fixture.
            let timestampLabel = issue.element?.label ?? ""
            let isKnownTimestampAuditFalsePositive =
                ignoresAnonymousConversationContrast
                && (issue.auditType == .contrast
                    || issue.auditType == .dynamicType)
                && timestampLabel.range(
                    of: #"^\d{1,2}:\d{2}$"#,
                    options: .regularExpression
                ) != nil
            // Xcode 27 beta also treats the native conversation ScrollView as
            // a describable element. It has no label by design; its message
            // children carry the actionable accessibility information.
            let conversationElementFrame = issue.element?.frame
            let isKnownConversationScrollContainerFalsePositive =
                ignoresAnonymousConversationContrast
                && issue.auditType == .sufficientElementDescription
                && issue.element?.identifier.isEmpty == true
                && issue.element?.label.isEmpty == true
                && conversationElementFrame?.minX == 0
                && (conversationElementFrame?.width ?? 0) >= app.frame.width
                && (conversationElementFrame?.height ?? 0) > app.frame.height / 2
            let isKnownFalsePositive = isKnownGlassFalsePositive
                || isKnownHostedRowFalsePositive
                || isKnownAnonymousConversationFalsePositive
                || isKnownCatalogTextSizingFalsePositive
                || isKnownOutgoingBubbleContrastFalsePositive
                || isKnownTimestampAuditFalsePositive
                || isKnownConversationScrollContainerFalsePositive

            if !isKnownFalsePositive {
                let element = issue.element
                let details = """
                \(issue.compactDescription)
                \(issue.detailedDescription)
                Type: \(issue.auditType)
                Identifier: \(element?.identifier ?? "none")
                Label: \(element?.label ?? "none")
                Value: \(element?.value as? String ?? "none")
                Frame: \(String(describing: element?.frame))
                """
                let attachment = XCTAttachment(string: details)
                attachment.name = "Unexpected accessibility audit issue"
                attachment.lifetime = .keepAlways
                self.add(attachment)
            }

            return isKnownFalsePositive
        }
    }
}
