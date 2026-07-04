package com.example.pillreminder.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile PillDao _pillDao;

  private volatile DoseLogDao _doseLogDao;

  private volatile InteractionRuleDao _interactionRuleDao;

  private volatile DrugHistoryDao _drugHistoryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `pills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `photoUri` TEXT, `colorHex` TEXT NOT NULL, `doseAmount` REAL NOT NULL, `foodRelation` TEXT NOT NULL, `waitAfterMinutes` INTEGER NOT NULL, `timesOfDay` TEXT NOT NULL, `startDateEpochDay` INTEGER NOT NULL, `treatmentDurationDays` INTEGER, `inventoryCount` REAL, `lowStockThresholdDays` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `dose_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pillId` INTEGER NOT NULL, `scheduledAtMillis` INTEGER NOT NULL, `status` TEXT NOT NULL, `actionAtMillis` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `interaction_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pillAId` INTEGER NOT NULL, `pillBId` INTEGER NOT NULL, `minGapMinutes` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `drug_history` (`normalizedName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `doseAmount` REAL NOT NULL, `foodRelation` TEXT NOT NULL, `waitAfterMinutes` INTEGER NOT NULL, `timesOfDay` TEXT NOT NULL, `treatmentDurationDays` INTEGER, `lowStockThresholdDays` INTEGER NOT NULL, `lastUsedEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`normalizedName`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '55c61a2179d0c7f2661150396337f570')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `pills`");
        db.execSQL("DROP TABLE IF EXISTS `dose_logs`");
        db.execSQL("DROP TABLE IF EXISTS `interaction_rules`");
        db.execSQL("DROP TABLE IF EXISTS `drug_history`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPills = new HashMap<String, TableInfo.Column>(13);
        _columnsPills.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("photoUri", new TableInfo.Column("photoUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("colorHex", new TableInfo.Column("colorHex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("doseAmount", new TableInfo.Column("doseAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("foodRelation", new TableInfo.Column("foodRelation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("waitAfterMinutes", new TableInfo.Column("waitAfterMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("timesOfDay", new TableInfo.Column("timesOfDay", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("startDateEpochDay", new TableInfo.Column("startDateEpochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("treatmentDurationDays", new TableInfo.Column("treatmentDurationDays", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("inventoryCount", new TableInfo.Column("inventoryCount", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("lowStockThresholdDays", new TableInfo.Column("lowStockThresholdDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPills.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPills = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPills = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPills = new TableInfo("pills", _columnsPills, _foreignKeysPills, _indicesPills);
        final TableInfo _existingPills = TableInfo.read(db, "pills");
        if (!_infoPills.equals(_existingPills)) {
          return new RoomOpenHelper.ValidationResult(false, "pills(com.example.pillreminder.data.Pill).\n"
                  + " Expected:\n" + _infoPills + "\n"
                  + " Found:\n" + _existingPills);
        }
        final HashMap<String, TableInfo.Column> _columnsDoseLogs = new HashMap<String, TableInfo.Column>(5);
        _columnsDoseLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDoseLogs.put("pillId", new TableInfo.Column("pillId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDoseLogs.put("scheduledAtMillis", new TableInfo.Column("scheduledAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDoseLogs.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDoseLogs.put("actionAtMillis", new TableInfo.Column("actionAtMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDoseLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDoseLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDoseLogs = new TableInfo("dose_logs", _columnsDoseLogs, _foreignKeysDoseLogs, _indicesDoseLogs);
        final TableInfo _existingDoseLogs = TableInfo.read(db, "dose_logs");
        if (!_infoDoseLogs.equals(_existingDoseLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "dose_logs(com.example.pillreminder.data.DoseLog).\n"
                  + " Expected:\n" + _infoDoseLogs + "\n"
                  + " Found:\n" + _existingDoseLogs);
        }
        final HashMap<String, TableInfo.Column> _columnsInteractionRules = new HashMap<String, TableInfo.Column>(4);
        _columnsInteractionRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionRules.put("pillAId", new TableInfo.Column("pillAId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionRules.put("pillBId", new TableInfo.Column("pillBId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsInteractionRules.put("minGapMinutes", new TableInfo.Column("minGapMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysInteractionRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesInteractionRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoInteractionRules = new TableInfo("interaction_rules", _columnsInteractionRules, _foreignKeysInteractionRules, _indicesInteractionRules);
        final TableInfo _existingInteractionRules = TableInfo.read(db, "interaction_rules");
        if (!_infoInteractionRules.equals(_existingInteractionRules)) {
          return new RoomOpenHelper.ValidationResult(false, "interaction_rules(com.example.pillreminder.data.InteractionRule).\n"
                  + " Expected:\n" + _infoInteractionRules + "\n"
                  + " Found:\n" + _existingInteractionRules);
        }
        final HashMap<String, TableInfo.Column> _columnsDrugHistory = new HashMap<String, TableInfo.Column>(9);
        _columnsDrugHistory.put("normalizedName", new TableInfo.Column("normalizedName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("doseAmount", new TableInfo.Column("doseAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("foodRelation", new TableInfo.Column("foodRelation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("waitAfterMinutes", new TableInfo.Column("waitAfterMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("timesOfDay", new TableInfo.Column("timesOfDay", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("treatmentDurationDays", new TableInfo.Column("treatmentDurationDays", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("lowStockThresholdDays", new TableInfo.Column("lowStockThresholdDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDrugHistory.put("lastUsedEpochMillis", new TableInfo.Column("lastUsedEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDrugHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDrugHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDrugHistory = new TableInfo("drug_history", _columnsDrugHistory, _foreignKeysDrugHistory, _indicesDrugHistory);
        final TableInfo _existingDrugHistory = TableInfo.read(db, "drug_history");
        if (!_infoDrugHistory.equals(_existingDrugHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "drug_history(com.example.pillreminder.data.DrugHistory).\n"
                  + " Expected:\n" + _infoDrugHistory + "\n"
                  + " Found:\n" + _existingDrugHistory);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "55c61a2179d0c7f2661150396337f570", "51117ddddad20062d3c403f21de231fe");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "pills","dose_logs","interaction_rules","drug_history");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `pills`");
      _db.execSQL("DELETE FROM `dose_logs`");
      _db.execSQL("DELETE FROM `interaction_rules`");
      _db.execSQL("DELETE FROM `drug_history`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PillDao.class, PillDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DoseLogDao.class, DoseLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(InteractionRuleDao.class, InteractionRuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DrugHistoryDao.class, DrugHistoryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PillDao pillDao() {
    if (_pillDao != null) {
      return _pillDao;
    } else {
      synchronized(this) {
        if(_pillDao == null) {
          _pillDao = new PillDao_Impl(this);
        }
        return _pillDao;
      }
    }
  }

  @Override
  public DoseLogDao doseLogDao() {
    if (_doseLogDao != null) {
      return _doseLogDao;
    } else {
      synchronized(this) {
        if(_doseLogDao == null) {
          _doseLogDao = new DoseLogDao_Impl(this);
        }
        return _doseLogDao;
      }
    }
  }

  @Override
  public InteractionRuleDao interactionRuleDao() {
    if (_interactionRuleDao != null) {
      return _interactionRuleDao;
    } else {
      synchronized(this) {
        if(_interactionRuleDao == null) {
          _interactionRuleDao = new InteractionRuleDao_Impl(this);
        }
        return _interactionRuleDao;
      }
    }
  }

  @Override
  public DrugHistoryDao drugHistoryDao() {
    if (_drugHistoryDao != null) {
      return _drugHistoryDao;
    } else {
      synchronized(this) {
        if(_drugHistoryDao == null) {
          _drugHistoryDao = new DrugHistoryDao_Impl(this);
        }
        return _drugHistoryDao;
      }
    }
  }
}
