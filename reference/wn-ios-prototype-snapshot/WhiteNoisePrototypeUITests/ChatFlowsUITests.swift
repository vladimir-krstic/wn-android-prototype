import XCTest

@MainActor
final class ChatFlowsUITests: XCTestCase {
    private lazy var app: XCUIApplication = {
        let application = XCUIApplication()
        application.launchArguments = ["-ui-testing-chats"]
        application.launch()
        XCTAssertTrue(application.buttons["chats.profile"].waitForExistence(timeout: 8))
        return application
    }()

    override func setUp() {
        continueAfterFailure = false
    }

    func testDirectSendPersistsAndUpdatesRowPreview() {
        openChat("maya-chen")
        let composer = app.textViews["conversation.composer"]
        XCTAssertTrue(composer.waitForExistence(timeout: 5))
        composer.tap()
        composer.typeText("A message from the UI test")
        app.buttons["conversation.send"].tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "A message from the UI test")
        ).firstMatch.waitForExistence(timeout: 3))

        app.navigationBars.buttons.firstMatch.tap()
        let row = locateChat("maya-chen")
        XCTAssertTrue(row.label.contains("A message from the UI test"))

        row.tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "A message from the UI test")
        ).firstMatch.waitForExistence(timeout: 3))
    }

    func testComposerExpandsAndCollapsesWhenEmptyAndWithOneLine() {
        openChat("maya-chen")
        let composer = app.textViews["conversation.composer"]

        assertComposerExpandsAndCollapses(composer)

        composer.tap()
        composer.typeText("One line")
        XCTAssertEqual(composer.value as? String, "One line")
        assertComposerExpandsAndCollapses(composer)
    }

    func testShowcaseHistoriesOpenAtNewestAndReachTheirBeginnings() {
        openChat("maya-chen")
        let latestDirect = app.descendants(matching: .any)["message.maya-17"]
        XCTAssertTrue(latestDirect.waitForExistence(timeout: 3))
        XCTAssertTrue(latestDirect.isHittable)

        let firstDirect = app.descendants(matching: .any)["message.maya-1"]
        scrollToBeginning(until: firstDirect)
        XCTAssertTrue(firstDirect.exists)

        app.buttons["BackButton"].tap()
        openChat("weekend-walks")
        let latestGroup = app.descendants(matching: .any)["message.week-msg-27"]
        XCTAssertTrue(latestGroup.waitForExistence(timeout: 3))

        let createdEvent = app.descendants(matching: .any)["event.week-event-created"]
        scrollToBeginning(until: createdEvent)
        XCTAssertTrue(createdEvent.exists)
        XCTAssertEqual(createdEvent.label, "You created the group.")
    }

    func testNewDirectChatCreatesOnceAndPersistsMessage() {
        app.buttons["chats.new"].tap()
        XCTAssertTrue(app.navigationBars["New Chat"].waitForExistence(timeout: 3))
        let search = app.searchFields["Name or npub"]
        search.tap()
        search.typeText("Iris")
        button(containing: "Iris").tap()
        XCTAssertTrue(app.navigationBars["User Profile"].waitForExistence(timeout: 3))
        let message = app.buttons["person-profile.message"]
        XCTAssertTrue(message.waitForExistence(timeout: 3))
        XCTAssertTrue(message.isHittable)
        message.tap()

        let composer = app.textViews["conversation.composer"]
        XCTAssertTrue(composer.waitForExistence(timeout: 5))
        composer.tap()
        composer.typeText("First chat with Iris")
        XCTAssertEqual(composer.value as? String, "First chat with Iris")
        let send = app.buttons["conversation.send"]
        XCTAssertTrue(send.waitForExistence(timeout: 3))
        send.tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "First chat with Iris")
        ).firstMatch.waitForExistence(timeout: 3))

        returnToChats()
        let row = app.cells.matching(
            NSPredicate(format: "label CONTAINS %@", "Iris")
        ).firstMatch
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        XCTAssertTrue(row.label.contains("First chat with Iris"))
        row.tap()
        XCTAssertTrue(app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "First chat with Iris")
        ).firstMatch.waitForExistence(timeout: 3))
    }

    func testNewGroupCreatesAdminAndInitialEvent() {
        app.buttons["chats.new"].tap()
        XCTAssertTrue(app.navigationBars["New Chat"].waitForExistence(timeout: 3))
        app.buttons["New Group"].tap()
        XCTAssertTrue(app.navigationBars["New Group"].waitForExistence(timeout: 3))

        let peopleSearch = app.searchFields["Search People"]
        XCTAssertTrue(peopleSearch.waitForExistence(timeout: 3))
        peopleSearch.tap()
        peopleSearch.typeText("Maya Chen")
        button(containing: "Maya Chen").tap()
        peopleSearch.tap()
        app.buttons["Clear text"].tap()
        peopleSearch.typeText("Elias Moreno")
        button(containing: "Elias Moreno").tap()
        if app.buttons["close"].exists {
            app.buttons["close"].tap()
        }
        XCTAssertTrue(app.buttons["Continue"].waitForExistence(timeout: 3))
        app.buttons["Continue"].tap()

        let name = app.textFields["new-group.name"]
        XCTAssertTrue(name.waitForExistence(timeout: 3))
        name.tap()
        name.typeText("UI Test Walks")
        app.buttons["new-group.create"].tap()

        XCTAssertTrue(app.staticTexts["UI Test Walks"].firstMatch.waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["You created the group."].waitForExistence(timeout: 3))
        app.buttons["conversation.info"].tap()
        XCTAssertTrue(app.staticTexts["Admin"].waitForExistence(timeout: 3))
    }

    func testMayaRetryReplyReactDeleteAndSearch() {
        openChat("maya-chen")

        let failedMessage = app.descendants(matching: .any)["message.maya-16"]
        XCTAssertTrue(failedMessage.waitForExistence(timeout: 3))
        failedMessage.press(forDuration: 1)
        let retry = app.buttons["Retry Send"]
        XCTAssertTrue(retry.waitForExistence(timeout: 3))
        retry.tap()
        XCTAssertFalse(retry.exists)

        let incoming = app.descendants(matching: .any)["message.maya-17"]
        XCTAssertTrue(incoming.waitForExistence(timeout: 3))
        incoming.press(forDuration: 1)
        app.buttons["Reply"].tap()
        XCTAssertTrue(app.staticTexts.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Replying to")
        ).firstMatch.waitForExistence(timeout: 3))

        incoming.press(forDuration: 1)
        app.buttons["🦫"].tap()
        XCTAssertTrue(app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "🦫")
        ).firstMatch.waitForExistence(timeout: 3))

        let composer = app.textViews["conversation.composer"]
        composer.tap()
        composer.typeText("Delete this message")
        app.buttons["conversation.send"].tap()
        let sent = app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "Delete this message")
        ).firstMatch
        XCTAssertTrue(sent.waitForExistence(timeout: 3))
        sent.press(forDuration: 1)
        app.buttons["Delete"].tap()
        app.buttons["Delete for Everyone"].tap()
        XCTAssertTrue(app.staticTexts["You deleted this message."].waitForExistence(timeout: 3))

        app.buttons["conversation.info"].tap()
        XCTAssertTrue(app.buttons["Search"].waitForExistence(timeout: 3))
        app.buttons["Search"].tap()
        let search = app.searchFields["conversation.search.field"]
        XCTAssertTrue(search.waitForExistence(timeout: 3))
        search.tap()
        search.typeText("Weekend Notes")
        XCTAssertTrue(app.staticTexts["Weekend Notes.pdf"].firstMatch.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["1 of 1 match"].waitForExistence(timeout: 3))
    }

    func testWeekendGroupMetadataAndMemberManagement() {
        openChat("weekend-walks")
        app.buttons["conversation.info"].tap()
        XCTAssertTrue(app.navigationBars["Group Info"].waitForExistence(timeout: 3))
        reveal(app.buttons["Edit Group"]).tap()
        let name = app.textFields["edit-group.name"]
        XCTAssertTrue(name.waitForExistence(timeout: 3))
        name.tap()
        name.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 30))
        name.typeText("Discard This Name")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(revealFromTop(app.staticTexts["Weekend Walks"].firstMatch).exists)

        reveal(app.buttons["Edit Group"]).tap()
        XCTAssertTrue(name.waitForExistence(timeout: 3))
        name.tap()
        name.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 30))
        name.typeText("Saturday Walks")
        app.buttons["Save"].tap()
        XCTAssertTrue(revealFromTop(app.staticTexts["Saturday Walks"].firstMatch).exists)

        reveal(app.buttons["Add People"]).tap()
        let peopleSearch = app.searchFields["Search People"]
        XCTAssertTrue(peopleSearch.waitForExistence(timeout: 3))
        peopleSearch.tap()
        peopleSearch.typeText("Avery Stone")
        button(containing: "Avery Stone").tap()
        if app.buttons["close"].exists {
            app.buttons["close"].tap()
        }
        XCTAssertTrue(app.buttons["Add"].waitForExistence(timeout: 3))
        app.buttons["Add"].tap()
        let avery = app.buttons["group-member.avery-stone"]
        XCTAssertTrue(reveal(avery).exists)

        reveal(app.buttons["group-member.maya-chen"]).tap()
        XCTAssertTrue(app.navigationBars["Group Member"].waitForExistence(timeout: 3))
        reveal(app.buttons["Make Admin"]).tap()
        app.sheets.buttons["Make Admin"].tap()
        XCTAssertTrue(app.staticTexts["Admin"].waitForExistence(timeout: 3))
        app.buttons["Remove Admin"].tap()
        app.sheets.buttons["Remove Admin"].tap()

        app.navigationBars.buttons.firstMatch.tap()
        reveal(avery).tap()
        reveal(app.buttons["Remove from Group"]).tap()
        app.sheets.buttons["Remove from Group"].tap()
        XCTAssertTrue(app.navigationBars["Group Info"].waitForExistence(timeout: 3))
    }

    func testOnlyAdminCannotLeave() {
        openChat("weekend-walks")
        app.buttons["conversation.info"].tap()
        reveal(app.buttons["Leave Group"]).tap()
        XCTAssertTrue(app.alerts["Can’t Leave Group"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[
            "You’re the only admin in this group. Make another member an admin before you leave."
        ].exists)
        app.alerts.buttons["Done"].tap()
    }

    func testPromotingAnotherAdminAllowsLeavingGroup() {
        openChat("weekend-walks")
        app.buttons["conversation.info"].tap()
        reveal(app.buttons["group-member.maya-chen"]).tap()
        reveal(app.buttons["Make Admin"]).tap()
        tapConfirmationButton("Make Admin")
        app.navigationBars.buttons.firstMatch.tap()

        reveal(app.buttons["Leave Group"]).tap()
        tapConfirmationButton("Leave Group")
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.staticTexts["You left this group."].firstMatch.waitForExistence(timeout: 3))
        XCTAssertFalse(app.textViews["conversation.composer"].exists)
    }

    func testRemovingFinalChatRelayDisablesAndAddingRestoresComposer() {
        openChat("maya-chen")
        app.buttons["conversation.info"].tap()
        app.buttons["Relays"].tap()

        for _ in 0..<3 {
            let relay = app.buttons.matching(
                NSPredicate(format: "label CONTAINS %@", "wss://")
            ).firstMatch
            XCTAssertTrue(relay.waitForExistence(timeout: 2))
            relay.tap()
            XCTAssertTrue(app.buttons["Remove Relay"].waitForExistence(timeout: 2))
            app.buttons["Remove Relay"].tap()
            app.alerts.buttons["Remove Relay"].tap()
        }

        app.navigationBars.buttons.firstMatch.tap()
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.buttons["Check Chat Relays"].waitForExistence(timeout: 3))
        app.buttons["Check Chat Relays"].tap()
        XCTAssertTrue(app.buttons["Add Relay"].waitForExistence(timeout: 3))
        app.buttons["Add Relay"].tap()
        let input = app.textFields["chat-relays.input"]
        XCTAssertTrue(input.waitForExistence(timeout: 3))
        input.tap()
        input.typeText("wss://restored.example.com")
        app.buttons["Add"].tap()
        app.navigationBars.buttons.firstMatch.tap()
        XCTAssertTrue(app.textViews["conversation.composer"].waitForExistence(timeout: 3))
    }

    func testVoiceLongPressReviewsThenSendsAndWaveformBubblePlays() {
        openChat("maya-chen")
        let voice = app.descendants(matching: .any)["conversation.voice"]
        XCTAssertTrue(voice.waitForExistence(timeout: 3))
        voice.tap()
        XCTAssertFalse(app.buttons["conversation.voice.stop"].exists)
        voice.press(forDuration: 0.6)
        let stop = app.buttons["conversation.voice.stop"]
        XCTAssertTrue(stop.waitForExistence(timeout: 3))
        XCTAssertTrue(app.descendants(matching: .any)["conversation.voice.timer"].exists)
        stop.tap()
        let reviewToggle = app.buttons["conversation.voice.review.toggle"]
        XCTAssertTrue(reviewToggle.waitForExistence(timeout: 3))
        reviewToggle.tap()
        XCTAssertEqual(reviewToggle.label, "Pause Voice Message")
        reviewToggle.tap()
        app.buttons["conversation.voice.cancel"].tap()

        let voiceToSend = app.descendants(matching: .any)["conversation.voice"]
        XCTAssertTrue(voiceToSend.waitForExistence(timeout: 3))
        voiceToSend.press(forDuration: 0.6)
        XCTAssertTrue(stop.waitForExistence(timeout: 3))
        stop.tap()
        app.buttons["conversation.voice.review.send"].tap()
        let playbackToggle = app.buttons.matching(
            NSPredicate(
                format: "identifier BEGINSWITH %@",
                "voice."
            )
        ).firstMatch
        XCTAssertTrue(
            playbackToggle.waitForExistence(timeout: 3),
            "The newly sent voice message should expose its play control."
        )
        XCTAssertTrue(playbackToggle.isHittable)
        XCTAssertEqual(playbackToggle.label, "Play Voice Message")
        playbackToggle.tap()
        XCTAssertEqual(
            XCTWaiter.wait(
                for: [XCTNSPredicateExpectation(
                    predicate: NSPredicate(format: "label == %@", "Pause Voice Message"),
                    object: playbackToggle
                )],
                timeout: 2
            ),
            .completed
        )
        playbackToggle.tap()
        XCTAssertEqual(
            XCTWaiter.wait(
                for: [XCTNSPredicateExpectation(
                    predicate: NSPredicate(format: "label == %@", "Play Voice Message"),
                    object: playbackToggle
                )],
                timeout: 2
            ),
            .completed
        )

    }

    func testAttachmentMenuPreservesKeyboardAndBlocksComposerBeforeSelection() {
        openChat("maya-chen")
        let composer = app.textViews["conversation.composer"]
        let attachmentMenu = app.buttons["conversation.attachment-menu"]
        XCTAssertTrue(attachmentMenu.waitForExistence(timeout: 3))

        composer.tap()
        let keyboard = app.keyboards.firstMatch
        XCTAssertTrue(keyboard.waitForExistence(timeout: 3))

        attachmentMenu.tap()
        let camera = app.buttons["Camera"]
        let photosAndVideos = app.buttons["Photos and Videos"]
        let files = app.buttons["Files"]
        XCTAssertTrue(camera.waitForExistence(timeout: 3))
        XCTAssertTrue(photosAndVideos.exists)
        XCTAssertTrue(files.exists)
        XCTAssertLessThan(camera.frame.minY, photosAndVideos.frame.minY)
        XCTAssertLessThan(photosAndVideos.frame.minY, files.frame.minY)
        XCTAssertTrue(keyboard.exists)
        let keyboardDismissal = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == false"),
            object: keyboard
        )
        keyboardDismissal.isInverted = true
        XCTAssertEqual(
            XCTWaiter.wait(for: [keyboardDismissal], timeout: 1),
            .completed
        )

        files.tap()
        XCTAssertTrue(
            app.navigationBars["UIDocumentPickerView"].waitForExistence(timeout: 3)
        )
    }

    func testMediaPreviewControlsMatchNavigationGeometry() {
        openChat("maya-chen")
        app.buttons["conversation.info"].tap()

        let photosAndVideos = app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "Photos & Videos")
        ).firstMatch
        XCTAssertTrue(reveal(photosAndVideos).exists)
        photosAndVideos.tap()

        let firstMedia = app.buttons.matching(
            NSPredicate(format: "identifier BEGINSWITH %@", "chat-info.media.")
        ).firstMatch
        XCTAssertTrue(firstMedia.waitForExistence(timeout: 5))
        firstMedia.tap()

        let close = app.buttons["media-preview.close"]
        let more = app.buttons["media-preview.more"]
        let share = app.buttons["media-preview.share"]
        let forward = app.buttons["media-preview.forward"]
        for control in [close, more, share, forward] {
            XCTAssertTrue(control.waitForExistence(timeout: 5))
            XCTAssertTrue(control.isHittable)
        }

        XCTAssertEqual(share.frame.width, forward.frame.width, accuracy: 1)
        XCTAssertEqual(share.frame.height, forward.frame.height, accuracy: 1)
        // Toolbar accessibility frames don't include the complete rendered
        // glass halo. Compare alignment centers here; the retained screenshot
        // is the regression evidence for the visible glass surfaces.
        XCTAssertEqual(share.frame.midX, close.frame.midX, accuracy: 1)
        XCTAssertEqual(forward.frame.midX, more.frame.midX, accuracy: 1)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Media preview control geometry"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testForwardMediaSupportsSelectionMessageAndKeyboardDismissal() {
        openChat("maya-chen")
        app.buttons["conversation.info"].tap()

        let photosAndVideos = app.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "Photos & Videos")
        ).firstMatch
        XCTAssertTrue(reveal(photosAndVideos).exists)
        photosAndVideos.tap()

        let firstMedia = app.buttons.matching(
            NSPredicate(format: "identifier BEGINSWITH %@", "chat-info.media.")
        ).firstMatch
        XCTAssertTrue(firstMedia.waitForExistence(timeout: 5))
        firstMedia.tap()
        app.buttons["media-preview.forward"].tap()

        XCTAssertTrue(app.navigationBars["Forward To"].waitForExistence(timeout: 5))
        let destination = app.buttons["forward.chat.catalog-direct-text"]
        XCTAssertTrue(destination.waitForExistence(timeout: 3))
        destination.coordinate(
            withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)
        ).tap()
        XCTAssertEqual(destination.value as? String, "Selected")

        let message = app.descendants(matching: .any)["forward.message"]
        let send = app.buttons["forward.send"]
        XCTAssertTrue(message.waitForExistence(timeout: 3))
        XCTAssertTrue(send.waitForExistence(timeout: 3))
        XCTAssertTrue(send.isHittable)

        message.tap()
        message.typeText("A note with this image")
        XCTAssertTrue(app.keyboards.firstMatch.exists)

        let keyboardScreenshot = XCTAttachment(screenshot: app.screenshot())
        keyboardScreenshot.name = "Forward media composer with keyboard"
        keyboardScreenshot.lifetime = .keepAlways
        add(keyboardScreenshot)
        let secondDestination = app.buttons["forward.chat.catalog-direct-dates"]
        XCTAssertTrue(secondDestination.waitForExistence(timeout: 3))
        secondDestination.tap()
        XCTAssertFalse(app.keyboards.firstMatch.exists)

        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Forward media selected destination and message"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        send.tap()
        XCTAssertTrue(app.buttons["media-preview.forward"].waitForExistence(timeout: 3))
        app.buttons["media-preview.close"].tap()
        returnToChats()
        openChat("catalog-direct-text")
        XCTAssertTrue(app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "A note with this image")
        ).firstMatch.waitForExistence(timeout: 3))
    }

    private func openChat(_ id: String) {
        let row = locateChat(id)
        row.tap()
        XCTAssertTrue(app.textViews["conversation.composer"].waitForExistence(timeout: 5))
    }

    private func assertComposerExpandsAndCollapses(
        _ composer: XCUIElement,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let compactHeight = composer.frame.height
        composer.swipeUp()
        XCTAssertEqual(
            XCTWaiter.wait(
                for: [XCTNSPredicateExpectation(
                    predicate: NSPredicate { _, _ in
                        composer.frame.height > compactHeight + 80
                    },
                    object: composer
                )],
                timeout: 2
            ),
            .completed,
            "The composer should expand from its compact height.",
            file: file,
            line: line
        )

        composer.swipeDown()
        XCTAssertEqual(
            XCTWaiter.wait(
                for: [XCTNSPredicateExpectation(
                    predicate: NSPredicate { _, _ in
                        composer.frame.height <= compactHeight + 2
                    },
                    object: composer
                )],
                timeout: 2
            ),
            .completed,
            "The composer should return to its compact height.",
            file: file,
            line: line
        )
    }

    private func scrollToBeginning(until element: XCUIElement) {
        for _ in 0..<24 where !element.isHittable {
            app.swipeDown()
        }
        XCTAssertTrue(element.waitForExistence(timeout: 3))
    }

    private func returnToChats() {
        for _ in 0..<6 {
            if app.buttons["chats.profile"].exists { return }
            if app.buttons["close"].exists {
                app.buttons["close"].tap()
                continue
            }
            let back = app.buttons["BackButton"]
            XCTAssertTrue(back.waitForExistence(timeout: 3))
            back.tap()
        }
        XCTAssertTrue(app.buttons["chats.profile"].waitForExistence(timeout: 3))
    }

    private func locateChat(_ id: String) -> XCUIElement {
        let row = app.cells["chat.\(id)"]
        if !row.exists {
            app.buttons["Search Chats"].tap()
            let search = app.searchFields["Search Chats"]
            search.typeText(id == "weekend-walks" ? "Weekend Walks" : "Maya Chen")
        }
        XCTAssertTrue(row.waitForExistence(timeout: 5))
        return row
    }

    private func button(containing text: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func tapConfirmationButton(_ title: String) {
        let alertButton = app.alerts.buttons[title]
        if alertButton.waitForExistence(timeout: 2) {
            alertButton.tap()
            return
        }

        let sheetButton = app.sheets.buttons[title]
        XCTAssertTrue(sheetButton.waitForExistence(timeout: 2))
        sheetButton.tap()
    }

    @discardableResult
    private func reveal(_ element: XCUIElement) -> XCUIElement {
        for _ in 0..<10 {
            if element.exists, element.isHittable { return element }
            app.swipeUp()
        }
        XCTAssertTrue(element.exists)
        XCTAssertTrue(element.isHittable)
        return element
    }

    @discardableResult
    private func revealFromTop(_ element: XCUIElement) -> XCUIElement {
        for _ in 0..<8 {
            if element.exists, element.isHittable { return element }
            app.swipeDown()
        }
        XCTAssertTrue(element.exists)
        XCTAssertTrue(element.isHittable)
        return element
    }
}
