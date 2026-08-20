import SwiftUI
import UIKit
import UserNotifications

struct NotificationSettingsPrototypeView: View {
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase

    @State private var authorizationStatus: UNAuthorizationStatus?
    @State private var isShowingPermissionError = false

    @Binding var settings: PrototypeSettingsState

    var body: some View {
        Form {
            permissionSection

            Section {
                Toggle(
                    "Local Notifications",
                    isOn: localNotificationsBinding
                )
                .disabled(!notificationsAreAuthorized)
            } footer: {
                Text(
                    "Creates message notifications on this iPhone. Without Native Push, delivery may wait until White Noise is active."
                )
            }

            Section {
                Toggle(
                    "Native Push",
                    isOn: nativePushBinding
                )
                .disabled(
                    !notificationsAreAuthorized ||
                    !settings.localNotificationsEnabled
                )
            } footer: {
                Text(
                    "Uses a generic wake-up signal to check for new messages in the background. Message details stay on this iPhone."
                )
            }

            Section {
                Picker(
                    "Notification Preview",
                    selection: $settings.notificationPreview
                ) {
                    ForEach(PrototypeNotificationPreview.allCases) { mode in
                        Text(mode.rawValue)
                            .tag(mode)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()

                Label(
                    settings.notificationPreview.example,
                    systemImage: "bell.badge"
                )
                .font(.callout)
                .foregroundStyle(.secondary)
            } header: {
                Text("Preview")
            } footer: {
                Text(
                    "Choose how much message information appears on the Lock Screen."
                )
            }
            .disabled(
                !notificationsAreAuthorized ||
                !settings.localNotificationsEnabled
            )
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await refreshAuthorizationStatus()
        }
        .onChange(of: scenePhase) { _, newPhase in
            guard newPhase == .active else {
                return
            }

            Task {
                await refreshAuthorizationStatus()
            }
        }
        .alert(
            "Couldn’t Update Notifications",
            isPresented: $isShowingPermissionError
        ) {
            Button("Dismiss", role: .cancel) {}
        } message: {
            Text("Try again, or allow notifications in iOS Settings.")
        }
    }

    @ViewBuilder
    private var permissionSection: some View {
        switch authorizationStatus {
        case .notDetermined:
            Section {
                Label(
                    "Allow notifications to use these options.",
                    systemImage: "bell.badge"
                )

                Button("Allow Notifications") {
                    requestNotificationAuthorization()
                }
            }
        case .denied:
            Section {
                Label {
                    VStack(alignment: .leading) {
                        Text("Notifications are off")
                            .foregroundStyle(.primary)

                        Text(
                            "Turn them on in iOS Settings to use these options."
                        )
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    }
                } icon: {
                    Image(systemName: "bell.slash")
                        .foregroundStyle(.secondary)
                }

                Button("Open Settings") {
                    openNotificationSettings()
                }
            }
        default:
            EmptyView()
        }
    }

    private var notificationsAreAuthorized: Bool {
        switch authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            true
        default:
            false
        }
    }

    private var localNotificationsBinding: Binding<Bool> {
        Binding {
            notificationsAreAuthorized &&
            settings.localNotificationsEnabled
        } set: { isEnabled in
            settings.localNotificationsEnabled = isEnabled
            if !isEnabled {
                settings.nativePushEnabled = false
            }
        }
    }

    private var nativePushBinding: Binding<Bool> {
        Binding {
            notificationsAreAuthorized &&
            settings.localNotificationsEnabled &&
            settings.nativePushEnabled
        } set: { isEnabled in
            settings.nativePushEnabled = isEnabled
        }
    }

    private func refreshAuthorizationStatus() async {
        let notificationSettings =
            await UNUserNotificationCenter.current().notificationSettings()
        authorizationStatus = notificationSettings.authorizationStatus
    }

    private func requestNotificationAuthorization() {
        Task {
            do {
                _ = try await UNUserNotificationCenter.current()
                    .requestAuthorization(options: [.alert, .sound, .badge])
                await refreshAuthorizationStatus()
            } catch {
                isShowingPermissionError = true
            }
        }
    }

    private func openNotificationSettings() {
        guard let url = URL(
            string: UIApplication.openNotificationSettingsURLString
        ) else {
            return
        }

        openURL(url)
    }
}

struct AppearanceSettingsPrototypeView: View {
    @Binding var settings: PrototypeSettingsState

    var body: some View {
        Form {
            Section {
                Picker("Theme", selection: $settings.appearance) {
                    ForEach(PrototypeAppearance.allCases) { appearance in
                        Text(appearance.rawValue)
                            .tag(appearance)
                    }
                }
                .pickerStyle(.inline)
                .labelsHidden()
            } header: {
                Text("Theme")
            } footer: {
                Text(
                    "System follows your device appearance. Light and Dark keep the selected appearance."
                )
            }

            Section {
                Picker("Language", selection: $settings.language) {
                    ForEach(PrototypeLanguage.allCases) { language in
                        Text(language.title)
                            .tag(language)
                    }
                }
                .pickerStyle(.navigationLink)
            }
        }
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct DataUsagePrototypeView: View {
    @Binding var settings: PrototypeSettingsState

    var body: some View {
        Form {
            Section {
                ForEach(PrototypeMediaType.allCases) { type in
                    NavigationLink {
                        AutoDownloadPrototypeView(
                            type: type,
                            settings: $settings
                        )
                    } label: {
                        LabeledContent {
                            Text(
                                settings.autoDownload[type, default: .never]
                                    .rawValue
                            )
                        } label: {
                            Text(type.rawValue)
                        }
                    }
                }

                Button("Reset Auto-Download Settings") {
                    settings.autoDownload =
                        PrototypeSettingsState.defaultAutoDownload
                }
                .disabled(
                    settings.autoDownload ==
                        PrototypeSettingsState.defaultAutoDownload
                )
            } header: {
                Text("Auto-Download")
            } footer: {
                Text(
                    "Media that isn't downloaded automatically shows a download button."
                )
            }

            Section {
                NavigationLink {
                    SentMediaQualityPrototypeView(settings: $settings)
                } label: {
                    LabeledContent(
                        "Sent Media Quality",
                        value: settings.mediaQuality.rawValue
                    )
                }
            } header: {
                Text("Sent Media")
            } footer: {
                Text("Choose the quality for photos and videos you send.")
            }
        }
        .navigationTitle("Data Usage")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SentMediaQualityPrototypeView: View {
    @Binding var settings: PrototypeSettingsState

    var body: some View {
        List {
            Section {
                ForEach(PrototypeMediaQuality.allCases) { quality in
                    Button {
                        settings.mediaQuality = quality
                    } label: {
                        HStack {
                            Text(quality.rawValue)
                                .foregroundStyle(.primary)

                            Spacer()

                            if settings.mediaQuality == quality {
                                Image(systemName: "checkmark")
                                    .fontWeight(.semibold)
                            }
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(
                        settings.mediaQuality == quality
                            ? .isSelected
                            : []
                    )
                }
            } header: {
                Text("Photos and Videos")
            } footer: {
                Text(
                    "High sends uncompressed photos and videos for better quality, but uses more data. Standard compresses media to use less data."
                )
            }
        }
        .navigationTitle("Sent Media Quality")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AutoDownloadPrototypeView: View {
    let type: PrototypeMediaType
    @Binding var settings: PrototypeSettingsState

    var body: some View {
        List {
            Section {
                ForEach(PrototypeAutoDownloadLevel.allCases) { level in
                    Button {
                        settings.autoDownload[type] = level
                    } label: {
                        HStack {
                            Text(level.rawValue)
                                .foregroundStyle(.primary)
                            Spacer()
                            if settings.autoDownload[type] == level {
                                Image(systemName: "checkmark")
                                    .fontWeight(.semibold)
                            }
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .navigationTitle(type.rawValue)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Notifications") {
    @Previewable @State var settings = PrototypeSettingsState()

    NavigationStack {
        NotificationSettingsPrototypeView(settings: $settings)
    }
}

#Preview("Appearance") {
    @Previewable @State var settings = PrototypeSettingsState()

    NavigationStack {
        AppearanceSettingsPrototypeView(settings: $settings)
    }
}

#Preview("Data Usage") {
    @Previewable @State var settings = PrototypeSettingsState()

    NavigationStack {
        DataUsagePrototypeView(settings: $settings)
    }
}
