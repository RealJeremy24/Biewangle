package com.biewangle.dontforget.data.repository

import com.biewangle.dontforget.data.dao.MemoDao
import com.biewangle.dontforget.data.entity.toDomainModel
import com.biewangle.dontforget.data.entity.toEntity
import com.biewangle.dontforget.data.model.Memo
import com.biewangle.dontforget.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoRepository(private val memoDao: MemoDao) {

    fun getAllMemos(): Flow<List<Memo>> =
        memoDao.getAllMemos().map { list -> list.map { it.toDomainModel() } }

    fun getActiveReminders(): Flow<List<Memo>> =
        memoDao.getActiveReminders().map { list -> list.map { it.toDomainModel() } }

    suspend fun getTodayMemos(): List<Memo> {
        val now = DateTimeUtils.now()
        return memoDao.getMemosForDay(
            DateTimeUtils.getStartOfDay(now),
            DateTimeUtils.getEndOfDay(now)
        ).map { it.toDomainModel() }
    }

    suspend fun getById(id: Long): Memo? =
        memoDao.getById(id)?.toDomainModel()

    suspend fun insert(memo: Memo): Long =
        memoDao.insert(memo.toEntity())

    suspend fun update(memo: Memo) =
        memoDao.update(memo.toEntity())

    suspend fun delete(id: Long) =
        memoDao.deleteById(id)

    suspend fun toggleComplete(id: Long) {
        val entity = memoDao.getById(id) ?: return
        memoDao.setCompleted(id, !entity.isCompleted)
    }

    suspend fun getAllPendingReminders(): List<Memo> =
        memoDao.getPendingReminders().map { it.toDomainModel() }

    suspend fun rescheduleReminder(id: Long, newTime: Long) =
        memoDao.rescheduleReminder(id, newTime)

    fun getTotalCount(): Flow<Int> = memoDao.getTotalCount()
    fun getCompletedCount(): Flow<Int> = memoDao.getCompletedCount()
}
