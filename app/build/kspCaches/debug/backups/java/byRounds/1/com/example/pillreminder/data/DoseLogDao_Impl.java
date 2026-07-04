package com.example.pillreminder.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DoseLogDao_Impl implements DoseLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DoseLog> __insertionAdapterOfDoseLog;

  private final EntityDeletionOrUpdateAdapter<DoseLog> __updateAdapterOfDoseLog;

  public DoseLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDoseLog = new EntityInsertionAdapter<DoseLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `dose_logs` (`id`,`pillId`,`scheduledAtMillis`,`status`,`actionAtMillis`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DoseLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPillId());
        statement.bindLong(3, entity.getScheduledAtMillis());
        statement.bindString(4, __DoseStatus_enumToString(entity.getStatus()));
        if (entity.getActionAtMillis() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getActionAtMillis());
        }
      }
    };
    this.__updateAdapterOfDoseLog = new EntityDeletionOrUpdateAdapter<DoseLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `dose_logs` SET `id` = ?,`pillId` = ?,`scheduledAtMillis` = ?,`status` = ?,`actionAtMillis` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DoseLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPillId());
        statement.bindLong(3, entity.getScheduledAtMillis());
        statement.bindString(4, __DoseStatus_enumToString(entity.getStatus()));
        if (entity.getActionAtMillis() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getActionAtMillis());
        }
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final DoseLog log, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDoseLog.insertAndReturnId(log);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DoseLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDoseLog.handle(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object findByPillAndTime(final long pillId, final long scheduledAt,
      final Continuation<? super DoseLog> $completion) {
    final String _sql = "SELECT * FROM dose_logs WHERE pillId = ? AND scheduledAtMillis = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, pillId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, scheduledAt);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DoseLog>() {
      @Override
      @Nullable
      public DoseLog call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPillId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillId");
          final int _cursorIndexOfScheduledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAtMillis");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActionAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "actionAtMillis");
          final DoseLog _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPillId;
            _tmpPillId = _cursor.getLong(_cursorIndexOfPillId);
            final long _tmpScheduledAtMillis;
            _tmpScheduledAtMillis = _cursor.getLong(_cursorIndexOfScheduledAtMillis);
            final DoseStatus _tmpStatus;
            _tmpStatus = __DoseStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Long _tmpActionAtMillis;
            if (_cursor.isNull(_cursorIndexOfActionAtMillis)) {
              _tmpActionAtMillis = null;
            } else {
              _tmpActionAtMillis = _cursor.getLong(_cursorIndexOfActionAtMillis);
            }
            _result = new DoseLog(_tmpId,_tmpPillId,_tmpScheduledAtMillis,_tmpStatus,_tmpActionAtMillis);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DoseLog>> getLogsForDay(final long dayStart, final long dayEnd) {
    final String _sql = "SELECT * FROM dose_logs WHERE scheduledAtMillis BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayStart);
    _argIndex = 2;
    _statement.bindLong(_argIndex, dayEnd);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"dose_logs"}, new Callable<List<DoseLog>>() {
      @Override
      @NonNull
      public List<DoseLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPillId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillId");
          final int _cursorIndexOfScheduledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAtMillis");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActionAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "actionAtMillis");
          final List<DoseLog> _result = new ArrayList<DoseLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DoseLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPillId;
            _tmpPillId = _cursor.getLong(_cursorIndexOfPillId);
            final long _tmpScheduledAtMillis;
            _tmpScheduledAtMillis = _cursor.getLong(_cursorIndexOfScheduledAtMillis);
            final DoseStatus _tmpStatus;
            _tmpStatus = __DoseStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Long _tmpActionAtMillis;
            if (_cursor.isNull(_cursorIndexOfActionAtMillis)) {
              _tmpActionAtMillis = null;
            } else {
              _tmpActionAtMillis = _cursor.getLong(_cursorIndexOfActionAtMillis);
            }
            _item = new DoseLog(_tmpId,_tmpPillId,_tmpScheduledAtMillis,_tmpStatus,_tmpActionAtMillis);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object recentTakenForPill(final long pillId,
      final Continuation<? super List<DoseLog>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM dose_logs \n"
            + "        WHERE pillId = ? AND status = 'TAKEN' \n"
            + "        ORDER BY scheduledAtMillis DESC LIMIT 5\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, pillId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DoseLog>>() {
      @Override
      @NonNull
      public List<DoseLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPillId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillId");
          final int _cursorIndexOfScheduledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAtMillis");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfActionAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "actionAtMillis");
          final List<DoseLog> _result = new ArrayList<DoseLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DoseLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPillId;
            _tmpPillId = _cursor.getLong(_cursorIndexOfPillId);
            final long _tmpScheduledAtMillis;
            _tmpScheduledAtMillis = _cursor.getLong(_cursorIndexOfScheduledAtMillis);
            final DoseStatus _tmpStatus;
            _tmpStatus = __DoseStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Long _tmpActionAtMillis;
            if (_cursor.isNull(_cursorIndexOfActionAtMillis)) {
              _tmpActionAtMillis = null;
            } else {
              _tmpActionAtMillis = _cursor.getLong(_cursorIndexOfActionAtMillis);
            }
            _item = new DoseLog(_tmpId,_tmpPillId,_tmpScheduledAtMillis,_tmpStatus,_tmpActionAtMillis);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __DoseStatus_enumToString(@NonNull final DoseStatus _value) {
    switch (_value) {
      case PENDING: return "PENDING";
      case TAKEN: return "TAKEN";
      case SKIPPED: return "SKIPPED";
      case SNOOZED: return "SNOOZED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private DoseStatus __DoseStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "PENDING": return DoseStatus.PENDING;
      case "TAKEN": return DoseStatus.TAKEN;
      case "SKIPPED": return DoseStatus.SKIPPED;
      case "SNOOZED": return DoseStatus.SNOOZED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
