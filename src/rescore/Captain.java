/**
 * Jachtos kapitonas.
 * Objektai saugomi duombazės lentelėje Kapitonai.
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

public class Captain extends NamedEntity {
  private static Logger logger = Logger.getLogger(Captain.class.getName());
  private static PreparedStatement selectCaptain, selectAllCaptains, selectAllCaptainIds, deleteCaptain, updateName;
  private static TreeMap<Integer, WeakReference<NamedEntity> > captains = new TreeMap<Integer, WeakReference<NamedEntity> >();
  private static int captainsCount = -1; // kiek kapitonų yra duomenų bazėje

/**
 * Konstruktorius.
 * Naudojamas tik šioje klasėje. Norint gauti kapitono objektą iš kitur, naudoti
 * get() arba getAll().
 */
  private Captain(int id, String name) {
    this.id = id;
    this.name = name;
  }

/**
 * Šį konstruktorių kviečia statiniai NamedEntity klasės metodai,
 * imantys objektą iš duomenų bazės.
 *
 * @param id objekto ID
 * @param resultSet skaitymui paruošta duombazės eilutė su kitais objekto
 *                  kūrimui reikalingais laukais, kuriuos grąžina selectCaptain
 */
  public Captain(int id, ResultSet resultSet) throws SQLException {
    this(id, resultSet.getString(1));
  }

/**
 * Grąžina objektą kapitono su nurodytu id.
 *
 * @param id kapitono id
 * @return kapitonas su nurodytu id
 */
  public static Captain get(int id) {
    return (Captain)NamedEntity.get(id, selectCaptain, captains, Captain.class);
  }

/**
 * Grąžina visų kapitonų sąrašą jų id didėjimo tvarka.
 *
 * @return visų kapitonų sąrašas id didėjimo tvarka
 */
  public static List<Captain> getAll() {
    Vector<Captain> captainList = new Vector<Captain>();
    Captain captain;
    WeakReference<NamedEntity> weakReference;
    try {
      if (captainsCount == -1 || captains.size() / captainsCount > LIST_RATIO) {
        ResultSet resultSet = selectAllCaptains.executeQuery();
        for (captainsCount = 0; resultSet.next(); captainsCount++) {
          captain = null;
          weakReference = captains.get(resultSet.getInt(1));
          if (weakReference != null)
            captain = (Captain)weakReference.get();
          if (captain == null) {
            captain = new Captain(resultSet.getInt(1), resultSet.getString(2));
            captains.put(resultSet.getInt(1), new WeakReference<NamedEntity>(captain));
          }
          captainList.add(captain);
        }
      } else {
        ResultSet resultSet = selectAllCaptainIds.executeQuery();
        for (captainsCount = 0; resultSet.next(); captainsCount++) {
          captainList.add(get(resultSet.getInt(1)));
        }
      }
    } catch (SQLException exception) {
      logger.error("getAll SQL error: " + exception.getMessage());
      captainList = null;
    }
    return captainList;
  }

/**
 * Paruošia statinius PreparedStatement objektus.
 *
 * @param connection jungtis su duombaze
 */
  static void prepareStatements(Connection connection) {
    try {
      selectCaptain = connection.prepareStatement("SELECT Vardas FROM Kapitonai WHERE Id = ?");
      selectAllCaptains = connection.prepareStatement("SELECT Id, Vardas FROM Kapitonai ORDER BY Id");
      selectAllCaptainIds = connection.prepareStatement("SELECT Id FROM Kapitonai ORDER BY Id");
      updateName = connection.prepareStatement("UPDATE Kapitonai SET Vardas = ? WHERE Id = ?");
      deleteCaptain = connection.prepareStatement("DELETE FROM Kapitonai WHERE Id = ?");
    } catch (SQLException exception) {
      logger.error("prepareStatements SQL error: " + exception.getMessage());
    }
  }

  public boolean setName(String name) {
    return setName(name, updateName);
  }

  public boolean remove() {
    return remove(deleteCaptain);
  }

  protected TreeMap<Integer, WeakReference <NamedEntity> > getObjectMap() {
    return captains;
  }

}
