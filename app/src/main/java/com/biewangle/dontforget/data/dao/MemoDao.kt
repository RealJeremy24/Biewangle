package com.biewangle.dontforget.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.biewangle.dontforget.data.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos ORDER BY target_date DESC, reminder_time ASC")
    fun getAllMemos(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos ORDER BY target_date DESC, reminder_time ASC")
    suspend fun getAllMemosSnapshot(): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE target_date >= :dayStart AND target_date < :dayEnd ORDER BY reminder_time ASC")
    suspend fun getMemosForDay(dayStart: Long, dayEnd: Long): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: Long): MemoEntity?

    @Query("SELECT COUNT(*) FROM memos")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM memos WHERE is_completed = 1")
    fun getCompletedCount(): Flow<Int>

    /** 一次性获取总事项数（suspend） */
    @Query("SELECT COUNT(*) FROM memos")
    suspend fun getTotalCountOnce(): Int

    /** 一次性获取已完成数（suspend） */
    @Query("SELECT COUNT(*) FROM memos WHERE is_completed = 1")
    suspend fun getCompletedCountOnce(): Int

    // ── 统计详情查询 ──

    /** 获取目标日期在某时间点之后的备忘数量 */
    @Query("SELECT COUNT(*) FROM memos WHERE target_date >= :since")
    suspend fun getCountSince(since: Long): Int

    /** 获取目标日期在某时间点之后且已完成的备忘数量 */
    @Query("SELECT COUNT(*) FROM memos WHERE target_date >= :since AND is_completed = 1")
    suspend fun getCompletedCountSince(since: Long): Int

    /** 获取目标日期在某时间点之后的所有备忘（按目标日期倒序） */
    @Query("SELECT * FROM memos WHERE target_date >= :since ORDER BY target_date DESC")
    suspend fun getAllMemosSince(since: Long): List<MemoEntity>

    /** 获取目标日期在某时间点之后的已完成备忘录（按完成时间倒序） */
    @Query("SELECT * FROM memos WHERE is_completed = 1 AND target_date >= :since ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyCompleted(since: Long, limit: Int = 10): List<MemoEntity>

    // ── 范围查询（用于"今天"等需要严格日期区间的统计） ──

    /** 获取目标日期在 [start, end) 范围内的备忘数量 */
    @Query("SELECT COUNT(*) FROM memos WHERE target_date >= :start AND target_date < :end")
    suspend fun getCountBetween(start: Long, end: Long): Int

    /** 获取目标日期在 [start, end) 范围内且已完成的备忘数量 */
    @Query("SELECT COUNT(*) FROM memos WHERE target_date >= :start AND target_date < :end AND is_completed = 1")
    suspend fun getCompletedCountBetween(start: Long, end: Long): Int

    /** 获取目标日期在 [start, end) 范围内的所有备忘 */
    @Query("SELECT * FROM memos WHERE target_date >= :start AND target_date < :end ORDER BY target_date DESC")
    suspend fun getAllMemosBetween(start: Long, end: Long): List<MemoEntity>

    /** 获取目标日期在 [start, end) 范围内的已完成备忘录 */
    @Query("SELECT * FROM memos WHERE is_completed = 1 AND target_date >= :start AND target_date < :end ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyCompletedBetween(start: Long, end: Long, limit: Int = 10): List<MemoEntity>

    /** 获取所有已完成的备忘，按更新时间倒序 */
    @Query("SELECT * FROM memos WHERE is_completed = 1 ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecentlyCompletedAll(limit: Int = 10): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE is_completed = 0 AND reminder_time IS NOT NULL ORDER BY reminder_time ASC")
    suspend fun getPendingReminders(): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE is_completed = 0 AND (repeat_type != 'NONE' OR reminder_time IS NOT NULL) ORDER BY reminder_time ASC")
    fun getActiveReminders(): Flow<List<MemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memo: MemoEntity): Long

    @Update
    suspend fun update(memo: MemoEntity)

    @Delete
    suspend fun delete(memo: MemoEntity)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE memos SET is_completed = :completed, updated_at = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE memos SET reminder_time = :newTime, is_completed = 0, updated_at = :now WHERE id = :id")
    suspend fun rescheduleReminder(id: Long, newTime: Long, now: Long = System.currentTimeMillis())
}
