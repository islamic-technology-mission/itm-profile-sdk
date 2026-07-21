//  ProfileSDKDemoView.swift

import SwiftUI
import Profile_SDK

struct ProfileSDKDemoView: View {
    @StateObject private var vm = ProfileSDKDemoViewModel()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {

                    // Status bar
                    Text(vm.sdkStatus)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    // Profile selector
                    ProfileSelectorView(
                        profiles: vm.profiles,
                        selectedIndex: vm.selectedProfileIndex
                    ) { index in
                        vm.selectProfile(index)
                    }

                    // Profile section
                    SectionCard(title: "Profile") {
                        StateView(state: vm.profileState) { profile in
                            ProfileDetailView(profile: profile)
                        }
                        Button("Update Profile") { vm.updateProfile() }
                            .buttonStyle(SDKButtonStyle(color: .purple))
                            .disabled(profileIsNotLoaded)

                        if case .loading = vm.updateProfileState {
                            ProgressView("Updating…").padding(.top, 4)
                        } else if case .success(let p) = vm.updateProfileState {
                            Text("Updated: \(p.name ?? "—")").font(.caption).foregroundColor(.green)
                        } else if case .failure(let msg) = vm.updateProfileState {
                            Text("Error: \(msg)").font(.caption).foregroundColor(.red)
                        }
                    }

                    // Screen time section
                    SectionCard(title: "Screen Time") {
                        StateView(state: vm.screenTimeState) { entries in
                            ScreenTimeListView(entries: entries)
                        }
                        Button("Add Screen Time (+60s)") { vm.addScreenTime() }
                            .buttonStyle(SDKButtonStyle(color: Color(red: 0.02, green: 0.53, blue: 0.41)))
                            .disabled(profileIsNotLoaded)

                        if case .loading = vm.postScreenTimeState {
                            ProgressView("Posting…").padding(.top, 4)
                        } else if case .success(let msg) = vm.postScreenTimeState {
                            Text(msg).font(.caption).foregroundColor(.green)
                        } else if case .failure(let msg) = vm.postScreenTimeState {
                            Text("Error: \(msg)").font(.caption).foregroundColor(.red)
                        }
                    }

                    // Profile views section
                    SectionCard(title: "Profile Views") {
                        StateView(state: vm.profileViewsState) { data in
                            ProfileViewsView(data: data)
                        }
                        Button("Load Profile Views") { vm.loadProfileViews() }
                            .buttonStyle(SDKButtonStyle(color: Color(red: 0.49, green: 0.23, blue: 0.93)))
                            .disabled(profileIsNotLoaded)
                    }

                    // Nearby users section
                    SectionCard(title: "Nearby Users") {
                        StateView(state: vm.nearbyUsersState) { users in
                            NearbyUsersView(users: users)
                        }
                        Button("Load Nearby Users") { vm.loadNearbyUsers() }
                            .buttonStyle(SDKButtonStyle(color: Color(red: 0.03, green: 0.57, blue: 0.73)))
                            .disabled(profileIsNotLoaded)
                    }

                    Spacer(minLength: 32)
                }
                .padding(.vertical)
            }
            .navigationTitle("Islam360 SDK")
        }
        .onAppear {
            vm.selectProfile(0)
        }
    }

    private var profileIsNotLoaded: Bool {
        if case .success = vm.profileState { return false }
        return true
    }
}

// MARK: - Profile Selector

struct ProfileSelectorView: View {
    let profiles: [AppProfile]
    let selectedIndex: Int
    let onSelect: (Int) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(profiles.indices, id: \.self) { i in
                Button(profiles[i].label) { onSelect(i) }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(i == selectedIndex ? Color.purple : Color(.systemGray5))
                    .foregroundColor(i == selectedIndex ? .white : .primary)
                    .cornerRadius(8)
//                    .fontWeight(i == selectedIndex ? .bold : .regular)
            }
        }
        .padding(.horizontal)
    }
}

// MARK: - Generic State View

struct StateView<Value, Content: View>: View {
    let state: ViewState<Value>
    @ViewBuilder let content: (Value) -> Content

    var body: some View {
        switch state {
        case .idle:
            EmptyView()
        case .loading:
            HStack { Spacer(); ProgressView(); Spacer() }.padding(.vertical, 8)
        case .success(let value):
            content(value)
        case .empty:
            Text("No data available")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.vertical, 4)
        case .failure(let msg):
            Text("Error: \(msg)")
                .font(.caption)
                .foregroundColor(.red)
                .padding(.vertical, 4)
        }
    }
}

// MARK: - Profile Detail

struct ProfileDetailView: View {
    let profile: UserProfile

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(profile.name ?? "—").font(.title3).fontWeight(.bold)
            Text(profile.email ?? "—").font(.subheadline).foregroundColor(.secondary)
            Divider()
            ProfileRow(label: "Phone", value: profile.phone)
            ProfileRow(label: "Gender", value: profile.gender)
            ProfileRow(label: "DOB", value: profile.dob)
            if let loc = profile.location {
                ProfileRow(label: "Location", value: "\(loc.lat?.doubleValue ?? 0), \(loc.lng?.doubleValue ?? 0)")
            }
            ProfileRow(label: "Nearby", value: profile.nearbyUsers.map { "\($0.intValue)" })
            ProfileRow(label: "Joined", value: profile.createdAt)
            ProfileRow(label: "Visibility", value: profile.visibility)
        }
    }
}

struct ProfileRow: View {
    let label: String
    let value: String?

    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.purple)
                .frame(width: 80, alignment: .leading)
            Text(value ?? "—")
                .font(.caption)
                .foregroundColor(.primary)
            Spacer()
        }
    }
}

// MARK: - Screen Time

struct ScreenTimeListView: View {
    let entries: [ScreenTimeEntry]

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(entries, id: \.date) { entry in
                HStack {
                    Text(entry.date ?? "—").font(.caption).foregroundColor(.secondary)
                    Spacer()
                    Text("\(entry.minutes?.intValue ?? 0) min").font(.caption).fontWeight(.medium)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Profile Views

struct ProfileViewsView: View {
    let data: ProfileViewsData

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Total Views: \(data.total?.intValue ?? 0)")
                .font(.subheadline).fontWeight(.medium)
            if let items = data.items {
                ForEach(items, id: \.viewerUid) { viewer in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(viewer.name ?? "—").font(.caption).fontWeight(.medium)
                            Text(viewer.lastViewedAt ?? "—").font(.caption2).foregroundColor(.secondary)
                        }
                        Spacer()
                        Text("\(viewer.viewCount?.intValue ?? 0) views").font(.caption2)
                    }
                    Divider()
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Nearby Users

struct NearbyUsersView: View {
    let users: [NearbyUser]

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            ForEach(users, id: \.id) { user in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(user.name ?? "—").font(.caption).fontWeight(.medium)
                        Text("\(user.gender ?? "—") · \(user.visibility ?? "—")").font(.caption2).foregroundColor(.secondary)
                    }
                    Spacer()
                    if let loc = user.location {
                        Text("\(String(format: "%.4f", loc.lat?.doubleValue ?? 0)), \(String(format: "%.4f", loc.lng?.doubleValue ?? 0))")
                            .font(.caption2).foregroundColor(.secondary)
                    }
                }
                Divider()
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Section Card

struct SectionCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.headline)
                .padding(.bottom, 2)
            content()
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
        .padding(.horizontal)
    }
}

// MARK: - Button Style

struct SDKButtonStyle: ButtonStyle {
    let color: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(configuration.isPressed ? color.opacity(0.8) : color)
            .foregroundColor(.white)
            .cornerRadius(8)
            .opacity(configuration.isPressed ? 0.9 : 1.0)
    }
}
