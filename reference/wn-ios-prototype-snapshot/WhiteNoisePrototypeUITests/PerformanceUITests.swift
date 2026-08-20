import XCTest

@MainActor
final class PerformanceUITests: XCTestCase {
    private let visibleChatID = "catalog-direct-text"

    override func setUp() {
        continueAfterFailure = false
    }

    func testWelcomeLaunchPerformance() {
        let app = XCUIApplication()
        let options = XCTMeasureOptions()
        options.iterationCount = 5

        measure(
            metrics: [XCTApplicationLaunchMetric(waitUntilResponsive: true)],
            options: options
        ) {
            app.launch()
        }
    }

    func testSignedInLaunchPerformance() {
        let app = XCUIApplication()
        app.launchArguments = ["-ui-testing-chats"]
        let options = XCTMeasureOptions()
        options.iterationCount = 5

        measure(
            metrics: [XCTApplicationLaunchMetric(waitUntilResponsive: true)],
            options: options
        ) {
            app.launch()
        }
    }

    func testFirstConversationOpenPerformance() {
        let app = launchSignedInApp()
        let options = XCTMeasureOptions()
        options.iterationCount = 1
        var isDiscardedWarmUpInvocation = true

        measure(
            metrics: [
                XCTClockMetric(),
                XCTOSSignpostMetric.navigationTransitionMetric,
            ],
            options: options
        ) {
            // XCTest discards the first invocation. Keep it side-effect free
            // so the recorded invocation measures the real first open.
            if isDiscardedWarmUpInvocation {
                isDiscardedWarmUpInvocation = false
                return
            }
            openVisibleConversation(in: app)
        }
    }

    func testRepeatedConversationOpenPerformance() {
        let app = launchSignedInApp()
        openVisibleConversation(in: app)
        returnToChats(in: app)

        let options = XCTMeasureOptions()
        options.iterationCount = 5
        measure(
            metrics: [
                XCTClockMetric(),
                XCTOSSignpostMetric.navigationTransitionMetric,
            ],
            options: options
        ) {
            openVisibleConversation(in: app)
            returnToChats(in: app)
        }
    }

    private func launchSignedInApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-ui-testing-chats"]
        app.launch()
        XCTAssertTrue(
            app.buttons["chats.profile"].waitForExistence(timeout: 8)
        )
        XCTAssertTrue(
            app.cells["chat.\(visibleChatID)"].waitForExistence(timeout: 12)
        )
        return app
    }

    private func openVisibleConversation(in app: XCUIApplication) {
        let row = app.cells["chat.\(visibleChatID)"]
        XCTAssertTrue(row.waitForExistence(timeout: 8))
        row.tap()
        XCTAssertTrue(
            app.textViews["conversation.composer"]
                .waitForExistence(timeout: 8)
        )
    }

    private func returnToChats(in app: XCUIApplication) {
        let back = app.buttons["BackButton"]
        XCTAssertTrue(back.waitForExistence(timeout: 3))
        back.tap()
        XCTAssertTrue(
            app.cells["chat.\(visibleChatID)"].waitForExistence(timeout: 3)
        )
    }
}
