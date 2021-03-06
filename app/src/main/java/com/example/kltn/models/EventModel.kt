package com.example.kltn.models

import java.util.*

class EventModel(val id: Int) {
    var title: String = ""
    var description: String = ""
    var startTime: Date = Date()
    var endTime: Date = Date()
    var recurrenceType: Int = 0
    var groupId: Int = 0
    var participants: List<UserModel> = ArrayList<UserModel>()
}