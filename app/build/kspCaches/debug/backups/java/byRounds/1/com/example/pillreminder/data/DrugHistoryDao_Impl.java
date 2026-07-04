package com.example.pillreminder.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DrugHistoryDao_Impl implements DrugHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DrugHistory> __insertionAdapterOfDrugHistory;

  public DrugHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDrugHistory = new EntityInsertionAdapter<DrugHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `drug_history` (`normalizedName`,`displayName`,`doseAmount`,`foodRelation`,`waitAfterMinutes`,`timesOfDay`,`treatmentDurationDays`,`lowStockThresholdDays`,`lastUsedEpochMillis`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DrugHistory entity) {
        statement.bindString(1, entity.getNormalizedName());
        statement.bindString(2, entity.getDisplayName());
        statement.bindDouble(3, entity.getDoseAmount());
        statement.bindString(4, __FoodRelation_enumToString(entity.getFoodRelation()));
        statement.bindLong(5, entity.getWaitAfterMinutes());
        statement.bindString(6, entity.getTimesOfDay());
        if (entity.getTreatmentDurationDays() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTreatmentDurationDays());
        }
        statement.bindLong(8, entity.getLowStockThresholdDays());
        statement.bindLong(9, entity.getLastUsedEpochMillis());
      }
    };
  }

  @Override
  public Object upsert(final DrugHistory history, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDrugHistory.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object findByName(final String normalizedName,
      final Continuation<? super DrugHistory> $completion) {
    final String _sql = "SELECT * FROM drug_history WHERE normalizedName = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, normalizedName);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DrugHistory>() {
      @Override
      @Nullable
      public DrugHistory call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfNormalizedName = CursorUtil.getColumnIndexOrThrow(_cursor, "normalizedName");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfDoseAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "doseAmount");
          final int _cursorIndexOfFoodRelation = CursorUtil.getColumnIndexOrThrow(_cursor, "foodRelation");
          final int _cursorIndexOfWaitAfterMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "waitAfterMinutes");
          final int _cursorIndexOfTimesOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "timesOfDay");
          final int _cursorIndexOfTreatmentDurationDays = CursorUtil.getColumnIndexOrThrow(_cursor, "treatmentDurationDays");
          final int _cursorIndexOfLowStockThresholdDays = CursorUtil.getColumnIndexOrThrow(_cursor, "lowStockThresholdDays");
          final int _cursorIndexOfLastUsedEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsedEpochMillis");
          final DrugHistory _result;
          if (_cursor.moveToFirst()) {
            final String _tmpNormalizedName;
            _tmpNormalizedName = _cursor.getString(_cursorIndexOfNormalizedName);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final double _tmpDoseAmount;
            _tmpDoseAmount = _cursor.getDouble(_cursorIndexOfDoseAmount);
            final FoodRelation _tmpFoodRelation;
            _tmpFoodRelation = __FoodRelation_stringToEnum(_cursor.getString(_cursorIndexOfFoodRelation));
            final int _tmpWaitAfterMinutes;
            _tmpWaitAfterMinutes = _cursor.getInt(_cursorIndexOfWaitAfterMinutes);
            final String _tmpTimesOfDay;
            _tmpTimesOfDay = _cursor.getString(_cursorIndexOfTimesOfDay);
            final Integer _tmpTreatmentDurationDays;
            if (_cursor.isNull(_cursorIndexOfTreatmentDurationDays)) {
              _tmpTreatmentDurationDays = null;
            } else {
              _tmpTreatmentDurationDays = _cursor.getInt(_cursorIndexOfTreatmentDurationDays);
            }
            final int _tmpLowStockThresholdDays;
            _tmpLowStockThresholdDays = _cursor.getInt(_cursorIndexOfLowStockThresholdDays);
            final long _tmpLastUsedEpochMillis;
            _tmpLastUsedEpochMillis = _cursor.getLong(_cursorIndexOfLastUsedEpochMillis);
            _result = new DrugHistory(_tmpNormalizedName,_tmpDisplayName,_tmpDoseAmount,_tmpFoodRelation,_tmpWaitAfterMinutes,_tmpTimesOfDay,_tmpTreatmentDurationDays,_tmpLowStockThresholdDays,_tmpLastUsedEpochMillis);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __FoodRelation_enumToString(@NonNull final FoodRelation _value) {
    switch (_value) {
      case BEFORE_FOOD: return "BEFORE_FOOD";
      case AFTER_FOOD: return "AFTER_FOOD";
      case WITH_FOOD: return "WITH_FOOD";
      case NO_RELATION: return "NO_RELATION";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private FoodRelation __FoodRelation_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "BEFORE_FOOD": return FoodRelation.BEFORE_FOOD;
      case "AFTER_FOOD": return FoodRelation.AFTER_FOOD;
      case "WITH_FOOD": return FoodRelation.WITH_FOOD;
      case "NO_RELATION": return FoodRelation.NO_RELATION;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
