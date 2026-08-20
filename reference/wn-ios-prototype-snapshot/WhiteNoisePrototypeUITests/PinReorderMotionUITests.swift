import XCTest

@MainActor
final class PinReorderMotionUITests: XCTestCase {
    func testPinningLowerConversationReordersCleanly() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-ui-testing-chats"]
        app.launch()

        let reactions = app.cells["chat.catalog-direct-reactions"]
        XCTAssertTrue(reactions.waitForExistence(timeout: 8))
        reactions.swipeRight()

        let pin = app.buttons["Pin"].firstMatch
        XCTAssertTrue(pin.waitForExistence(timeout: 2))
        pin.tap()

        let dates = app.cells["chat.catalog-direct-dates"]
        XCTAssertTrue(dates.waitForExistence(timeout: 3))
        XCTAssertLessThan(reactions.frame.minY, dates.frame.minY)
    }
}
