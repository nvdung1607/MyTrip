package com.example.mytrip.data.db

import androidx.room.TypeConverter
import com.example.mytrip.data.db.entities.*

class Converters {
    @TypeConverter fun fromTripType(value: TripType): String = value.name
    @TypeConverter fun toTripType(value: String): TripType = TripType.valueOf(value)

    @TypeConverter fun fromTripStatus(value: TripStatus): String = value.name
    @TypeConverter fun toTripStatus(value: String): TripStatus = TripStatus.valueOf(value)

    @TypeConverter fun fromActivityStatus(value: ActivityStatus): String = value.name
    @TypeConverter fun toActivityStatus(value: String): ActivityStatus = ActivityStatus.valueOf(value)

    @TypeConverter fun fromNoteTag(value: NoteTag): String = value.name
    @TypeConverter fun toNoteTag(value: String): NoteTag = NoteTag.valueOf(value)

    @TypeConverter fun fromExpenseCategory(value: ExpenseCategory): String = value.name
    @TypeConverter fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
}
