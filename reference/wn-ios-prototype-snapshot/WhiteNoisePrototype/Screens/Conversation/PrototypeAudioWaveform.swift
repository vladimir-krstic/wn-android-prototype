import SwiftUI

struct PrototypeAudioWaveform: View {
    let samples: [Double]
    let progress: Double
    var attenuatesQuietSamples = false
    var unplayedOpacity = 0.38
    var barColor: Color? = nil

    private let barWidth: CGFloat = 2
    private let barSpacing: CGFloat = 2

    var body: some View {
        GeometryReader { geometry in
            let displayedBarCount = max(
                1,
                Int(
                    (geometry.size.width + barSpacing)
                        / (barWidth + barSpacing)
                )
            )
            let values = displayedSamples(count: displayedBarCount)
            let playedBarCount = Int(
                (Double(values.count) * min(max(progress, 0), 1)).rounded(.down)
            )

            HStack(alignment: .center, spacing: barSpacing) {
                ForEach(Array(values.enumerated()), id: \.offset) { index, value in
                    Group {
                        if let barColor {
                            Capsule().fill(barColor)
                        } else {
                            Capsule()
                        }
                    }
                        .frame(
                            width: barWidth,
                            height: max(3, geometry.size.height * CGFloat(value))
                        )
                        .opacity(
                            (index < playedBarCount ? 1 : unplayedOpacity)
                                * quietSampleOpacity(for: value)
                        )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .accessibilityHidden(true)
    }

    private func displayedSamples(count displayedBarCount: Int) -> [Double] {
        let clamped = samples.map { min(max($0, 0.08), 1) }
        if clamped.count >= displayedBarCount {
            return Array(clamped.suffix(displayedBarCount))
        }
        return Array(repeating: 0.08, count: displayedBarCount - clamped.count) + clamped
    }

    private func quietSampleOpacity(for value: Double) -> Double {
        guard attenuatesQuietSamples else { return 1 }
        return min(1, max(0.28, value))
    }
}

enum PrototypeWaveformSamples {
    static func samples(seed: String, count: Int = 42) -> [Double] {
        let scalarSeed = seed.unicodeScalars.reduce(0) { partial, scalar in
            (partial &* 31 &+ Int(scalar.value)) % 997
        }

        return (0..<count).map { index in
            let first = sin(Double(index + scalarSeed) * 0.61)
            let second = sin(Double(index * 3 + scalarSeed) * 0.17)
            return min(1, max(0.12, 0.2 + abs(first * second) * 0.8))
        }
    }

    static func liveSample(at tick: Int) -> Double {
        let first = sin(Double(tick) * 0.73)
        let second = sin(Double(tick) * 0.19 + 1.4)
        return min(1, max(0.1, 0.18 + abs(first * second) * 0.82))
    }
}
