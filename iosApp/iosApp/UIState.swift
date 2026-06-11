//  UIState.swift

import Foundation

enum ViewState<Value> {
    case idle
    case loading
    case success(Value)
    case empty
    case failure(String)
}
