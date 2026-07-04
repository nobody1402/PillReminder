package com.example.pillreminder.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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
public final class InteractionRuleDao_Impl implements InteractionRuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<InteractionRule> __insertionAdapterOfInteractionRule;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public InteractionRuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfInteractionRule = new EntityInsertionAdapter<InteractionRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `interaction_rules` (`id`,`pillAId`,`pillBId`,`minGapMinutes`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final InteractionRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPillAId());
        statement.bindLong(3, entity.getPillBId());
        statement.bindLong(4, entity.getMinGapMinutes());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM interaction_rules WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final InteractionRule rule, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfInteractionRule.insertAndReturnId(rule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<InteractionRule>> getAll() {
    final String _sql = "SELECT * FROM interaction_rules";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"interaction_rules"}, new Callable<List<InteractionRule>>() {
      @Override
      @NonNull
      public List<InteractionRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPillAId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillAId");
          final int _cursorIndexOfPillBId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillBId");
          final int _cursorIndexOfMinGapMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "minGapMinutes");
          final List<InteractionRule> _result = new ArrayList<InteractionRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final InteractionRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPillAId;
            _tmpPillAId = _cursor.getLong(_cursorIndexOfPillAId);
            final long _tmpPillBId;
            _tmpPillBId = _cursor.getLong(_cursorIndexOfPillBId);
            final int _tmpMinGapMinutes;
            _tmpMinGapMinutes = _cursor.getInt(_cursorIndexOfMinGapMinutes);
            _item = new InteractionRule(_tmpId,_tmpPillAId,_tmpPillBId,_tmpMinGapMinutes);
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
  public Object getRulesForPill(final long pillId,
      final Continuation<? super List<InteractionRule>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM interaction_rules \n"
            + "        WHERE pillAId = ? OR pillBId = ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, pillId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, pillId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<InteractionRule>>() {
      @Override
      @NonNull
      public List<InteractionRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPillAId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillAId");
          final int _cursorIndexOfPillBId = CursorUtil.getColumnIndexOrThrow(_cursor, "pillBId");
          final int _cursorIndexOfMinGapMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "minGapMinutes");
          final List<InteractionRule> _result = new ArrayList<InteractionRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final InteractionRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPillAId;
            _tmpPillAId = _cursor.getLong(_cursorIndexOfPillAId);
            final long _tmpPillBId;
            _tmpPillBId = _cursor.getLong(_cursorIndexOfPillBId);
            final int _tmpMinGapMinutes;
            _tmpMinGapMinutes = _cursor.getInt(_cursorIndexOfMinGapMinutes);
            _item = new InteractionRule(_tmpId,_tmpPillAId,_tmpPillBId,_tmpMinGapMinutes);
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
}
