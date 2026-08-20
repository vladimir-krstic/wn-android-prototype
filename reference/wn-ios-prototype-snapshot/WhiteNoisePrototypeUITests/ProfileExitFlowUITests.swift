import XCTest

@MainActor
final class ProfileExitFlowUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testSwitchingProfileReturnsToSettings() throws {
        let app = XCUIApplication()
        launchSignedInApp(app)
        addSecondProfile(in: app)
        openSettings(in: app)

        let switchProfile = app.buttons["Switch Profile"]
        XCTAssertTrue(switchProfile.waitForExistence(timeout: 3))
        switchProfile.tap()

        let marmota = app.buttons["profile-switcher.profile.marmota"]
        XCTAssertTrue(marmota.waitForExistence(timeout: 3))
        marmota.tap()

        XCTAssertTrue(
            app.descendants(matching: .any)["settings.screen"]
                .waitForExistence(timeout: 3)
        )
        XCTAssertTrue(app.navigationBars["Settings"].exists)
        XCTAssertTrue(app.staticTexts["Marmota"].exists)
    }

    func testSignOutKeepsDataAndRoutesToProfileSwitcher() throws {
        let app = XCUIApplication()
        launchSignedInApp(app)
        addSecondProfile(in: app)
        openSettings(in: app)
        openSignOut(in: app)

        turnOffWipeData(in: app)
        let signOut = app.buttons["sign-out.keep-data"]
        XCTAssertTrue(signOut.waitForExistence(timeout: 3))
        signOut.tap()

        XCTAssertTrue(
            app.descendants(matching: .any)["profile-switcher.list"]
                .waitForExistence(timeout: 5)
        )
        XCTAssertTrue(app.navigationBars["Switch Profile"].exists)
        XCTAssertFalse(app.buttons["welcome.sign-up"].exists)

        let marmota = app.buttons["profile-switcher.profile.marmota"]
        XCTAssertTrue(marmota.waitForExistence(timeout: 3))
        marmota.tap()

        XCTAssertTrue(
            app.descendants(matching: .any)["settings.screen"]
                .waitForExistence(timeout: 3)
        )
        XCTAssertTrue(app.navigationBars["Settings"].exists)
        XCTAssertTrue(app.staticTexts["Marmota"].exists)
    }

    func testSigningOutOnlyProfileRoutesToWelcome() throws {
        let app = XCUIApplication()
        launchSignedInApp(app)
        openSettings(in: app)
        openSignOut(in: app)

        turnOffWipeData(in: app)
        let signOut = app.buttons["sign-out.keep-data"]
        XCTAssertTrue(signOut.waitForExistence(timeout: 3))
        signOut.tap()

        XCTAssertTrue(
            app.buttons["welcome.sign-up"].waitForExistence(timeout: 5)
        )
        XCTAssertFalse(
            app.descendants(matching: .any)["profile-switcher.list"].exists
        )
    }

    func testWipingProfileWithAnotherSignedInProfileRoutesToSwitcher() throws {
        let app = XCUIApplication()
        launchSignedInApp(app)
        addSecondProfile(in: app)
        openSettings(in: app)
        openSignOut(in: app)

        confirmProfileWipe(named: "Pebble", in: app)

        XCTAssertTrue(
            app.descendants(matching: .any)["profile-switcher.list"]
                .waitForExistence(timeout: 5)
        )
        XCTAssertTrue(app.navigationBars["Switch Profile"].exists)
        XCTAssertFalse(app.buttons["welcome.sign-up"].exists)
    }

    func testWipingOnlyProfileRoutesToWelcome() throws {
        let app = XCUIApplication()
        launchSignedInApp(app)
        openSettings(in: app)
        openSignOut(in: app)

        confirmProfileWipe(named: "Marmota", in: app)

        XCTAssertTrue(
            app.buttons["welcome.sign-up"].waitForExistence(timeout: 5)
        )
        XCTAssertFalse(
            app.descendants(matching: .any)["profile-switcher.list"].exists
        )
    }

    private func launchSignedInApp(_ app: XCUIApplication) {
        app.launch()

        let signUp = app.buttons["welcome.sign-up"]
        XCTAssertTrue(signUp.waitForExistence(timeout: 3))
        signUp.tap()

        let createProfile = app.buttons["sign-up.create"]
        XCTAssertTrue(createProfile.waitForExistence(timeout: 3))
        createProfile.tap()

        XCTAssertTrue(
            app.buttons["chats.profile"].waitForExistence(timeout: 5)
        )
    }

    private func addSecondProfile(in app: XCUIApplication) {
        openSettings(in: app)

        let addProfile = app.buttons["Add Profile"]
        XCTAssertTrue(addProfile.waitForExistence(timeout: 3))
        addProfile.tap()

        let signUp = app.buttons["welcome.sign-up"]
        XCTAssertTrue(signUp.waitForExistence(timeout: 3))
        signUp.tap()

        let createProfile = app.buttons["sign-up.create"]
        XCTAssertTrue(createProfile.waitForExistence(timeout: 3))
        createProfile.tap()
        XCTAssertTrue(createProfile.waitForNonExistence(timeout: 5))

        XCTAssertTrue(
            app.navigationBars["Settings"].isHittable
                || app.buttons["chats.profile"].waitForExistence(timeout: 5)
        )
    }

    private func openSettings(in app: XCUIApplication) {
        if app.navigationBars["Settings"].isHittable {
            return
        }

        let profile = app.buttons["chats.profile"]
        XCTAssertTrue(profile.waitForExistence(timeout: 3))
        profile.tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
    }

    private func openSignOut(in app: XCUIApplication) {
        let signOut = app.descendants(matching: .any)["settings.signOut"]
        for _ in 0..<6 where !signOut.isHittable {
            app.swipeUp()
        }

        XCTAssertTrue(signOut.waitForExistence(timeout: 3))
        signOut.tap()
        XCTAssertTrue(app.navigationBars["Sign Out"].waitForExistence(timeout: 3))
    }

    private func confirmProfileWipe(
        named profileName: String,
        in app: XCUIApplication
    ) {
        let field = app.textFields["wipe-profile.confirmation-field"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        field.tap()
        for character in profileName {
            field.typeText(String(character))
        }
        let done = app.keyboards.buttons["Done"]
        if done.waitForExistence(timeout: 2) {
            done.tap()
        }

        let confirm = app.buttons["wipe-profile.confirm"]
        XCTAssertEqual(
            XCTWaiter.wait(
                for: [XCTNSPredicateExpectation(
                    predicate: NSPredicate(format: "isEnabled == true"),
                    object: confirm
                )],
                timeout: 3
            ),
            .completed
        )
        confirm.tap()
    }

    private func turnOffWipeData(in app: XCUIApplication) {
        let toggle = app.switches["sign-out.wipe-data-toggle"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 3))
        if toggle.value as? String != "0" {
            toggle.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)).tap()
        }
        XCTAssertEqual(toggle.value as? String, "0")
    }
}
