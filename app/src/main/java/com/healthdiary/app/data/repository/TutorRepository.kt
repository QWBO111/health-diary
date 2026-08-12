package com.healthdiary.app.data.repository

import com.healthdiary.app.data.local.AppDatabase
import com.healthdiary.app.data.local.TutorIncomeEntity
import com.healthdiary.app.data.local.TutorScheduleEntity
import kotlinx.coroutines.flow.Flow

class TutorRepository(private val db: AppDatabase) {

    fun incomeByDate(date: String): Flow<List<TutorIncomeEntity>> =
        db.tutorDao().observeIncomeByDate(date)

    val allIncome: Flow<List<TutorIncomeEntity>> = db.tutorDao().observeAllIncome()
    val allSchedule: Flow<List<TutorScheduleEntity>> = db.tutorDao().observeAllSchedule()

    suspend fun addIncome(
        date: String,
        studentName: String,
        subject: String,
        startMinute: Int,
        durationMin: Int,
        income: Float,
        note: String
    ): Long = db.tutorDao().insertIncome(
        TutorIncomeEntity(
            date = date,
            studentName = studentName,
            subject = subject,
            startMinute = startMinute,
            durationMin = durationMin,
            income = income,
            note = note
        )
    )

    suspend fun updateIncome(
        id: Long,
        studentName: String,
        subject: String,
        startMinute: Int,
        durationMin: Int,
        income: Float,
        note: String
    ) {
        db.tutorDao().getAllIncome().firstOrNull { it.id == id }?.let { record ->
            db.tutorDao().updateIncome(
                record.copy(
                    studentName = studentName,
                    subject = subject,
                    startMinute = startMinute,
                    durationMin = durationMin,
                    income = income,
                    note = note
                )
            )
        }
    }

    suspend fun deleteIncome(id: Long) {
        db.tutorDao().deleteIncome(id)
    }

    suspend fun addSchedule(
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        studentName: String,
        subject: String,
        note: String
    ): Long = db.tutorDao().insertSchedule(
        TutorScheduleEntity(
            weekday = weekday,
            startMinute = startMinute,
            endMinute = endMinute,
            studentName = studentName,
            subject = subject,
            note = note
        )
    )

    suspend fun updateSchedule(
        id: Long,
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        studentName: String,
        subject: String,
        note: String
    ) {
        db.tutorDao().getAllSchedule().firstOrNull { it.id == id }?.let { item ->
            db.tutorDao().updateSchedule(
                item.copy(
                    weekday = weekday,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    studentName = studentName,
                    subject = subject,
                    note = note
                )
            )
        }
    }

    suspend fun deleteSchedule(id: Long) {
        db.tutorDao().deleteSchedule(id)
    }

    /** 检查同一星期内是否有时间重叠的课程（编辑时排除自己） */
    suspend fun hasConflict(
        weekday: Int,
        startMinute: Int,
        endMinute: Int,
        excludeId: Long = -1L
    ): Boolean {
        if (startMinute >= endMinute) return true
        return db.tutorDao().getAllSchedule().any { item ->
            item.id != excludeId &&
                item.weekday == weekday &&
                startMinute < item.endMinute &&
                endMinute > item.startMinute
        }
    }
}
