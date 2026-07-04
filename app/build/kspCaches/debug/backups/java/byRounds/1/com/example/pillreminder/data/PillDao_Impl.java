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
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
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
public final class PillDao_Impl implements PillDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Pill> __insertionAdapterOfPill;

  private final EntityDeletionOrUpdateAdapter<Pill> __updateAdapterOfPill;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfUpdateInventory;

  public PillDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPill = new EntityInsertionAdapter<Pill>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `pills` (`id`,`name`,`photoUri`,`colorHex`,`doseAmount`,`foodRelation`,`waitAfterMinutes`,`timesOfDay`,`startDateEpochDay`,`treatmentDurationDays`,`inventoryCount`,`lowStockThresholdDays`,`isActive`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Pill entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getPhotoUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhotoUri());
        }
        statement.bindString(4, entity.getColorHex());
        statement.bindDouble(5, entity.getDoseAmount());
        statement.bindString(6, __FoodRelation_enumToString(entity.getFoodRelation()));
        statement.bindLong(7, entity.getWaitAfterMinutes());
        statement.bindString(8, entity.getTimesOfDay());
        statement.bindLong(9, entity.getStartDateEpochDay());
        if (entity.getTreatmentDurationDays() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getTreatmentDurationDays());
        }
        if (entity.getInventoryCount() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getInventoryCount());
        }
        statement.bindLong(12, entity.getLowStockThresholdDays());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(13, _tmp);
      }
    };
    this.__updateAdapterOfPill = new EntityDeletionOrUpdateAdapter<Pill>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pills` SET `id` = ?,`name` = ?,`photoUri` = ?,`colorHex` = ?,`doseAmount` = ?,`foodRelation` = ?,`waitAfterMinutes` = ?,`timesOfDay` = ?,`startDateEpochDay` = ?,`treatmentDurationDays` = ?,`inventoryCount` = ?,`lowStockThresholdDays` = ?,`isActive` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Pill entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getPhotoUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhotoUri());
        }
        statement.bindString(4, entity.getColorHex());
        statement.bindDouble(5, entity.getDoseAmount());
        statement.bindString(6, __FoodRelation_enumToString(entity.getFoodRelation()));
        statement.bindLong(7, entity.getWaitAfterMinutes());
        statement.bindString(8, entity.getTimesOfDay());
        statement.bindLong(9, entity.getStartDateEpochDay());
        if (entity.getTreatmentDurationDays() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getTreatmentDurationDays());
        }
        if (entity.getInventoryCount() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getInventoryCount());
        }
        statement.bindLong(12, entity.getLowStockThresholdDays());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pills WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateInventory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE pills SET inventoryCount = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Pill pill, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPill.insertAndReturnId(pill);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Pill pill, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPill.handle(pill);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
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
  public Object updateInventory(final long id, final double count,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateInventory.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, count);
        _argIndex = 2;
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
          __preparedStmtOfUpdateInventory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Pill>> getAllActive() {
    final String _sql = "SELECT * FROM pills WHERE isActive = 1 ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pills"}, new Callable<List<Pill>>() {
      @Override
      @NonNull
      public List<Pill> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfDoseAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "doseAmount");
          final int _cursorIndexOfFoodRelation = CursorUtil.getColumnIndexOrThrow(_cursor, "foodRelation");
          final int _cursorIndexOfWaitAfterMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "waitAfterMinutes");
          final int _cursorIndexOfTimesOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "timesOfDay");
          final int _cursorIndexOfStartDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateEpochDay");
          final int _cursorIndexOfTreatmentDurationDays = CursorUtil.getColumnIndexOrThrow(_cursor, "treatmentDurationDays");
          final int _cursorIndexOfInventoryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "inventoryCount");
          final int _cursorIndexOfLowStockThresholdDays = CursorUtil.getColumnIndexOrThrow(_cursor, "lowStockThresholdDays");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final List<Pill> _result = new ArrayList<Pill>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Pill _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final double _tmpDoseAmount;
            _tmpDoseAmount = _cursor.getDouble(_cursorIndexOfDoseAmount);
            final FoodRelation _tmpFoodRelation;
            _tmpFoodRelation = __FoodRelation_stringToEnum(_cursor.getString(_cursorIndexOfFoodRelation));
            final int _tmpWaitAfterMinutes;
            _tmpWaitAfterMinutes = _cursor.getInt(_cursorIndexOfWaitAfterMinutes);
            final String _tmpTimesOfDay;
            _tmpTimesOfDay = _cursor.getString(_cursorIndexOfTimesOfDay);
            final long _tmpStartDateEpochDay;
            _tmpStartDateEpochDay = _cursor.getLong(_cursorIndexOfStartDateEpochDay);
            final Integer _tmpTreatmentDurationDays;
            if (_cursor.isNull(_cursorIndexOfTreatmentDurationDays)) {
              _tmpTreatmentDurationDays = null;
            } else {
              _tmpTreatmentDurationDays = _cursor.getInt(_cursorIndexOfTreatmentDurationDays);
            }
            final Double _tmpInventoryCount;
            if (_cursor.isNull(_cursorIndexOfInventoryCount)) {
              _tmpInventoryCount = null;
            } else {
              _tmpInventoryCount = _cursor.getDouble(_cursorIndexOfInventoryCount);
            }
            final int _tmpLowStockThresholdDays;
            _tmpLowStockThresholdDays = _cursor.getInt(_cursorIndexOfLowStockThresholdDays);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _item = new Pill(_tmpId,_tmpName,_tmpPhotoUri,_tmpColorHex,_tmpDoseAmount,_tmpFoodRelation,_tmpWaitAfterMinutes,_tmpTimesOfDay,_tmpStartDateEpochDay,_tmpTreatmentDurationDays,_tmpInventoryCount,_tmpLowStockThresholdDays,_tmpIsActive);
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
  public Object getById(final long id, final Continuation<? super Pill> $completion) {
    final String _sql = "SELECT * FROM pills WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Pill>() {
      @Override
      @Nullable
      public Pill call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfDoseAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "doseAmount");
          final int _cursorIndexOfFoodRelation = CursorUtil.getColumnIndexOrThrow(_cursor, "foodRelation");
          final int _cursorIndexOfWaitAfterMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "waitAfterMinutes");
          final int _cursorIndexOfTimesOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "timesOfDay");
          final int _cursorIndexOfStartDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateEpochDay");
          final int _cursorIndexOfTreatmentDurationDays = CursorUtil.getColumnIndexOrThrow(_cursor, "treatmentDurationDays");
          final int _cursorIndexOfInventoryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "inventoryCount");
          final int _cursorIndexOfLowStockThresholdDays = CursorUtil.getColumnIndexOrThrow(_cursor, "lowStockThresholdDays");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final Pill _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final double _tmpDoseAmount;
            _tmpDoseAmount = _cursor.getDouble(_cursorIndexOfDoseAmount);
            final FoodRelation _tmpFoodRelation;
            _tmpFoodRelation = __FoodRelation_stringToEnum(_cursor.getString(_cursorIndexOfFoodRelation));
            final int _tmpWaitAfterMinutes;
            _tmpWaitAfterMinutes = _cursor.getInt(_cursorIndexOfWaitAfterMinutes);
            final String _tmpTimesOfDay;
            _tmpTimesOfDay = _cursor.getString(_cursorIndexOfTimesOfDay);
            final long _tmpStartDateEpochDay;
            _tmpStartDateEpochDay = _cursor.getLong(_cursorIndexOfStartDateEpochDay);
            final Integer _tmpTreatmentDurationDays;
            if (_cursor.isNull(_cursorIndexOfTreatmentDurationDays)) {
              _tmpTreatmentDurationDays = null;
            } else {
              _tmpTreatmentDurationDays = _cursor.getInt(_cursorIndexOfTreatmentDurationDays);
            }
            final Double _tmpInventoryCount;
            if (_cursor.isNull(_cursorIndexOfInventoryCount)) {
              _tmpInventoryCount = null;
            } else {
              _tmpInventoryCount = _cursor.getDouble(_cursorIndexOfInventoryCount);
            }
            final int _tmpLowStockThresholdDays;
            _tmpLowStockThresholdDays = _cursor.getInt(_cursorIndexOfLowStockThresholdDays);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _result = new Pill(_tmpId,_tmpName,_tmpPhotoUri,_tmpColorHex,_tmpDoseAmount,_tmpFoodRelation,_tmpWaitAfterMinutes,_tmpTimesOfDay,_tmpStartDateEpochDay,_tmpTreatmentDurationDays,_tmpInventoryCount,_tmpLowStockThresholdDays,_tmpIsActive);
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
  public Object getAllWithInventoryTracking(final Continuation<? super List<Pill>> $completion) {
    final String _sql = "SELECT * FROM pills WHERE isActive = 1 AND inventoryCount IS NOT NULL";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Pill>>() {
      @Override
      @NonNull
      public List<Pill> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfDoseAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "doseAmount");
          final int _cursorIndexOfFoodRelation = CursorUtil.getColumnIndexOrThrow(_cursor, "foodRelation");
          final int _cursorIndexOfWaitAfterMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "waitAfterMinutes");
          final int _cursorIndexOfTimesOfDay = CursorUtil.getColumnIndexOrThrow(_cursor, "timesOfDay");
          final int _cursorIndexOfStartDateEpochDay = CursorUtil.getColumnIndexOrThrow(_cursor, "startDateEpochDay");
          final int _cursorIndexOfTreatmentDurationDays = CursorUtil.getColumnIndexOrThrow(_cursor, "treatmentDurationDays");
          final int _cursorIndexOfInventoryCount = CursorUtil.getColumnIndexOrThrow(_cursor, "inventoryCount");
          final int _cursorIndexOfLowStockThresholdDays = CursorUtil.getColumnIndexOrThrow(_cursor, "lowStockThresholdDays");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final List<Pill> _result = new ArrayList<Pill>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Pill _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPhotoUri;
            if (_cursor.isNull(_cursorIndexOfPhotoUri)) {
              _tmpPhotoUri = null;
            } else {
              _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            }
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final double _tmpDoseAmount;
            _tmpDoseAmount = _cursor.getDouble(_cursorIndexOfDoseAmount);
            final FoodRelation _tmpFoodRelation;
            _tmpFoodRelation = __FoodRelation_stringToEnum(_cursor.getString(_cursorIndexOfFoodRelation));
            final int _tmpWaitAfterMinutes;
            _tmpWaitAfterMinutes = _cursor.getInt(_cursorIndexOfWaitAfterMinutes);
            final String _tmpTimesOfDay;
            _tmpTimesOfDay = _cursor.getString(_cursorIndexOfTimesOfDay);
            final long _tmpStartDateEpochDay;
            _tmpStartDateEpochDay = _cursor.getLong(_cursorIndexOfStartDateEpochDay);
            final Integer _tmpTreatmentDurationDays;
            if (_cursor.isNull(_cursorIndexOfTreatmentDurationDays)) {
              _tmpTreatmentDurationDays = null;
            } else {
              _tmpTreatmentDurationDays = _cursor.getInt(_cursorIndexOfTreatmentDurationDays);
            }
            final Double _tmpInventoryCount;
            if (_cursor.isNull(_cursorIndexOfInventoryCount)) {
              _tmpInventoryCount = null;
            } else {
              _tmpInventoryCount = _cursor.getDouble(_cursorIndexOfInventoryCount);
            }
            final int _tmpLowStockThresholdDays;
            _tmpLowStockThresholdDays = _cursor.getInt(_cursorIndexOfLowStockThresholdDays);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _item = new Pill(_tmpId,_tmpName,_tmpPhotoUri,_tmpColorHex,_tmpDoseAmount,_tmpFoodRelation,_tmpWaitAfterMinutes,_tmpTimesOfDay,_tmpStartDateEpochDay,_tmpTreatmentDurationDays,_tmpInventoryCount,_tmpLowStockThresholdDays,_tmpIsActive);
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
