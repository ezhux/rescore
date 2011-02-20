/**
 * Esybės, turinčios ID ir pavadinimus.
 */
package rescore;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public abstract class NamedEntity {
  private static Logger logger = Logger.getLogger(NamedEntity.class.getName());
  private static PreparedStatement lastInsertId;
  protected static double LIST_RATIO = 0.5;
  // jeigu objectMap turi mažesnę nei LIST_RATIO dalį visų įrašų arba visų
  // įrašų skaičius nežinomas, tada duombazės užklausia visų įrašų; priešingu
  // atveju – visų ID, pagal kuriuos gauna trūkstamas esybes po vieną
  protected int id;
  protected String name;

/**
 * Grąžina esybę pagal jos ID.
 *
 * @param id        esybės ID
 * @param select    paruošta užklausa esybės iš duomazės pagal ID gavimui
 * @param objectMap jau gautų iš duombazės objektų nuorodos
 * @param subClass  konkrečios esybės klasė, turinti konstruktorių, kuriam
 *                  perduodamas ID ir ResultSet
 * @return jeigu yra anksčiau gauta esybė su duotu ID – grąžina ją;
 *         jeigu ją paėmė iš duombazės – grąžina rezultatą (ResultSet);
 *         jeigu esybės su duotu ID nėra duombazėje, grąžina null
 */
  protected static NamedEntity get(int id, PreparedStatement select, Map<Integer, WeakReference<NamedEntity> > objectMap, Class<? extends NamedEntity> subClass) {
    WeakReference<NamedEntity> weakReference = objectMap.get(id);
    NamedEntity namedEntity = null;
    if (weakReference != null)
      namedEntity = weakReference.get();
    if (namedEntity == null) {
      try {
        select.setInt(1, id);
        ResultSet resultSet = select.executeQuery();
        if (resultSet.next()) {
          namedEntity = subClass.getConstructor(int.class, ResultSet.class).newInstance(id, resultSet);
          objectMap.put(namedEntity.getId(), new WeakReference<NamedEntity>(namedEntity));
        } else {
          logger.warn("Entity not found in the database");
        }
      } catch (SQLException exception) {
        logger.error("get SQL error: " + exception.getMessage());
      } catch (Exception exception) {
        logger.error("get non-SQL error: " + exception.getMessage());
      }
    }
    return namedEntity;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  protected boolean setName(String name, PreparedStatement updateName) {
    boolean ret = false;
    try {
      if (name == null)
        updateName.setNull(1, java.sql.Types.VARCHAR);
      else
        updateName.setString(1, name);
      updateName.setInt(2, id);
      int rowsAffected = updateName.executeUpdate();
      if (rowsAffected == 1) {
        this.name = name;
        ret = true;
      } else {
        logger.warn("Strange setName updated database rows count: " + rowsAffected);
      }
    } catch (SQLException exception) {
      logger.error("setName SQL error: " + exception.getMessage());
    }
    return ret;
  }

  abstract public boolean setName(String name);

  /**
   * Panaikina esybę iš duomenų bazės.
   * Toliau šis objektas nebeturėtų būti naudojamas.
   *
   * @param erase užklausa atitinkamos lentelės eilutės šalinimui
   * @return true, jei objektas panaikintas, false – jei įvyko klaida arba
   *         objektas buvo panaikintas anksčiau
   */
  protected boolean remove(PreparedStatement erase) {
    if (id != 0) {
      try {
        erase.setInt(1, id);
        int rowsDeleted = erase.executeUpdate();
        if (rowsDeleted == 1) {
          getObjectMap().remove(id);
          id = 0;
          return true;
        } else {
          logger.warn("Strange remove deleted database rows count: " + rowsDeleted);
        }
      } catch (SQLException exception) {
        logger.error("remove SQL error: " + exception.getMessage());
      }
    }
    return false;
  }

  abstract public boolean remove();

  static protected int getLastInsertId() {
    try {
      ResultSet resultSet = lastInsertId.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt(1);
      } else {
        logger.error("lastInsertId returned empty set");
      }
    } catch (SQLException exception) {
      logger.error("getLastInsertId SQL error: " + exception.getMessage());
    }
    return 0;
  }

/**
 * Paruošia statinius PreparedStatement objektus.
 *
 * @param connection jungtis su duombaze
 */
  static void prepareStatements(Connection connection) {
    try {
      lastInsertId = connection.prepareStatement("SELECT LAST_INSERT_ID()");
    } catch (SQLException exception) {
      logger.error("prepareStatements SQL error: " + exception.getMessage());
    }
  }

  abstract protected Map<Integer, WeakReference <NamedEntity> > getObjectMap();

  protected void finalize () {
    getObjectMap().remove(id);
  }
}
