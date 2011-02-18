/**
 * Jachtos savininkas.
 * Objektai saugomi duombazės lentelėje Savininkai.
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

public class Owner extends NamedEntity {
  private static Logger logger = Logger.getLogger(Owner.class.getName());
  private static PreparedStatement selectOwner, selectAllOwners, selectAllOwnerIds, deleteOwner, updateName;
  private static TreeMap<Integer, WeakReference<? extends NamedEntity> > owners = new TreeMap<Integer, WeakReference<? extends NamedEntity> >();
  private static int ownersCount = -1; // kiek savininkų yra duomenų bazėje

/**
 * Konstruktorius.
 * Naudojamas tik šioje klasėje. Norint gauti savininko objektą iš kitur,
 * naudoti get() arba getAll().
 */
  private Owner(int id, String name) {
    this.id = id;
    this.name = name;
  }

/**
 * Grąžina objektą savininko su nurodytu id.
 *
 * @param id savininko id
 * @return savininkas su nurodytu id
 */
  public static Owner get(int id) {
    WeakReference<? extends NamedEntity> weakReference = owners.get(id);
    Owner owner = null;
    if (weakReference != null)
      owner = (Owner)weakReference.get();
    if (owner == null) {
      try {
        selectOwner.setInt(1, id);
        ResultSet resultSet = selectOwner.executeQuery();
        if (resultSet.next()) {
          owner = new Owner(id, resultSet.getString(1));
          owners.put(id, new WeakReference<Owner>(owner));
        }
      } catch (SQLException exception) {
        logger.error("get SQL error: " + exception.getMessage());
      }
    }
    return owner;
  }

/**
 * Grąžina visų savininkų sąrašą jų id didėjimo tvarka.
 *
 * @return visų savininkų sąrašas id didėjimo tvarka
 */
  public static List<Owner> getAll() {
    Vector<Owner> ownerList = new Vector<Owner>();
    Owner owner;
    WeakReference<? extends NamedEntity> weakReference;
    try {
      if (ownersCount == -1 || owners.size() / ownersCount > LIST_RATIO) {
        ResultSet resultSet = selectAllOwners.executeQuery();
        for (ownersCount = 0; resultSet.next(); ownersCount++) {
          owner = null;
          weakReference = owners.get(resultSet.getInt(1));
          if (weakReference != null)
            owner = (Owner)weakReference.get();
          if (owner == null) {
            owner = new Owner(resultSet.getInt(1), resultSet.getString(2));
            owners.put(resultSet.getInt(1), new WeakReference<Owner>(owner));
          }
          ownerList.add(owner);
        }
      } else {
        ResultSet resultSet = selectAllOwnerIds.executeQuery();
        for (ownersCount = 0; resultSet.next(); ownersCount++) {
          ownerList.add(get(resultSet.getInt(1)));
        }
      }
    } catch (SQLException exception) {
      logger.error("getAll SQL error: " + exception.getMessage());
      ownerList = null;
    }
    return ownerList;
  }

/**
 * Paruošia statinius PreparedStatement objektus.
 *
 * @param connection jungtis su duombaze
 */
  static void prepareStatements(Connection connection) {
    try {
      selectOwner = connection.prepareStatement("SELECT Vardas FROM Savininkai WHERE Id = ?");
      selectAllOwners = connection.prepareStatement("SELECT Id, Vardas FROM Savininkai ORDER BY Id");
      selectAllOwnerIds = connection.prepareStatement("SELECT Id FROM Savininkai ORDER BY Id");
      updateName = connection.prepareStatement("UPDATE Savininkai SET Vardas = ? WHERE Id = ?");
      deleteOwner = connection.prepareStatement("DELETE FROM Savininkai WHERE Id = ?");
    } catch (SQLException exception) {
      logger.error("prepareStatements SQL error: " + exception.getMessage());
    }
  }

  public boolean setName(String name) {
    return setName(name, updateName);
  }

  public boolean remove() {
    return remove(deleteOwner);
  }

  protected TreeMap<Integer, WeakReference <? extends NamedEntity> > getObjectMap() {
    return owners;
  }

}
