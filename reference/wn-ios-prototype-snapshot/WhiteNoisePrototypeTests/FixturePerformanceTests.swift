import XCTest
@testable import WhiteNoisePrototype

@MainActor
final class FixturePerformanceTests: XCTestCase {
    func testFixtureGraphConstructionPerformance() {
        let now = Date(timeIntervalSince1970: 1_786_759_200)
        let options = XCTMeasureOptions()
        options.iterationCount = 5

        measure(
            metrics: [XCTClockMetric()],
            options: options
        ) {
            let people = PrototypeChatFixtures.people()
            let chats = PrototypeChatFixtures.chats(
                profileID: PrototypeProfile.marmotaID,
                relayURLs: ["wss://relay.example.com"],
                now: now
            )

            XCTAssertEqual(chats.count, 77)
            XCTAssertEqual(chats.filter(\.listState.isArchived).count, 5)
            XCTAssertFalse(people.isEmpty)
        }
    }
}
