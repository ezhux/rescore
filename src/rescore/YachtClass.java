/**
 * Jachtų modeliai.
 * Objektai saugomi duombazės lentelėje Modeliai.
 */
package rescore;

import java.lang.ref.WeakReference;
import java.util.TreeMap;
import java.util.List;
import java.util.Vector;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

public class YachtClass extends NamedEntity {
  private static Logger logger = Logger.getLogger(YachtClass.class.getName());
  private static PreparedStatement selectYachtClass, selectAllYachtClasses, selectAllYachtClassIds, updateName, deleteYachtClass;
  private static TreeMap<Integer, WeakReference<? extends NamedEntity> > yachtClasses = new TreeMap<Integer, WeakReference<? extends NamedEntity> >();
  private static int yachtClassesCount = -1; // kiek modelių yra duomenų bazėje

/**
 * Konstruktorius.
 * Naudojamas tik šioje klasėje. Norint gauti modelio objektą iš kitur, naudoti
 * get() arba getAll().
 */
  private YachtClass(int id, String name) {
    this.id = id;
    this.name = name;
  }

/**
 * Grąžina objektą modelio su nurodytu id.
 *
 * @param id modelio id
 * @return modelis su nurodytu id
 */
  public static YachtClass get(int id) {
    WeakReference<? extends NamedEntity> weakReference = yachtClasses.get(id);
    YachtClass yachtClass = null;
    if (weakReference != null)
      yachtClass = (YachtClass)weakReference.get();
    if (yachtClass == null) {
      try {
        selectYachtClass.setInt(1, id);
        ResultSet resultSet = selectYachtClass.executeQuery();
        if (resultSet.next()) {
          yachtClass = new YachtClass(id, resultSet.getString(1));
          yachtClasses.put(id, new WeakReference<YachtClass>(yachtClass));
        }
      } catch (SQLException exception) {
        logger.error("get SQL error: " + exception.getMessage());
      }
    }
    return yachtClass;
  }

/**
 * Grąžina visų modelių sąrašą jų id didėjimo tvarka.
 *
 * @return visų modelių sąrašas id didėjimo tvarka
 */
  public static List<YachtClass> getAll() {
    Vector<YachtClass> yachtClassList = new Vector<YachtClass>();
    YachtClass yachtClass;
    WeakReference<? extends NamedEntity> weakReference;
    try {
      if (yachtClassesCount == -1 || yachtClasses.size() / yachtClassesCount > LIST_RATIO) {
        ResultSet resultSet = selectAllYachtClasses.executeQuery();
        for (yachtClassesCount = 0; resultSet.next(); yachtClassesCount++) {
          yachtClass = null;
          weakReference = yachtClasses.get(resultSet.getInt(1));
          if (weakReference != null)
            yachtClass = (YachtClass)weakReference.get();
          if (yachtClass == null) {
            yachtClass = new YachtClass(resultSet.getInt(1), resultSet.getString(2));
            yachtClasses.put(resultSet.getInt(1), new WeakReference<YachtClass>(yachtClass));
          }
          yachtClassList.add(yachtClass);
        }
      } else {
        ResultSet resultSet = selectAllYachtClassIds.executeQuery();
        for (yachtClassesCount = 0; resultSet.next(); yachtClassesCount++) {
          yachtClassList.add(get(resultSet.getInt(1)));
        }
      }
    } catch (SQLException exception) {
      logger.error("getAll SQL error: " + exception.getMessage());
      yachtClassList = null;
    }
    return yachtClassList;
  }

/**
 * Paruošia statinius PreparedStatement objektus.
 *
 * @param connection jungtis su duombaze
 */
  static void prepareStatements(Connection connection) {
    try {
      selectYachtClass = connection.prepareStatement("SELECT Pavadinimas FROM Modeliai WHERE Id = ?");
      selectAllYachtClasses = connection.prepareStatement("SELECT Id, Pavadinimas FROM Modeliai ORDER BY Id");
      selectAllYachtClassIds = connection.prepareStatement("SELECT Id FROM Modeliai ORDER BY Id");
      updateName = connection.prepareStatement("UPDATE Modeliai SET Pavadinimas = ? WHERE Id = ?");
      deleteYachtClass = connection.prepareStatement("DELETE FROM Modeliai WHERE Id = ?");
    } catch (SQLException exception) {
      logger.error("prepareStatements SQL error: " + exception.getMessage());
    }
  }

  public boolean setName(String name) {
    return setName(name, updateName);
  }

  public boolean remove() {
    return remove(deleteYachtClass);
  }

  protected TreeMap<Integer, WeakReference <? extends NamedEntity> > getObjectMap() {
    return yachtClasses;
  }
}
