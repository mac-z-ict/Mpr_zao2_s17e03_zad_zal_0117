package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dao.mappers.IMapResultSetToEntity;
import dao.uow.IUnitOfWork;
import domain.Permission;
import domain.Role;
import domain.User;

public class PermissionRepository extends RepositoryBase<Permission> implements IPermissionRepository {
	
	public static final String TABLE_NAME = "permission";
	private static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "("
			+ "id bigint GENERATED BY DEFAULT AS IDENTITY,"
			+ "name VARCHAR(32)"
			+ ")";
	private static final String INSERT_QUERY = "INSERT INTO " + TABLE_NAME + "(name) VALUES(?)";
	private static final String UPDATE_QUERY = "UPDATE " + TABLE_NAME + " SET name = ? WHERE id = ?";
	private static final String BY_ROLE_QUERY = "SELECT DISTINCT P.* FROM " + TABLE_NAME + " P "
			+ "INNER JOIN " + RolePermissionRepository.TABLE_NAME + " RP ON(RP.PERMISSION_ID = P.ID) "
					+ "WHERE RP.ROLE_ID = ?";
	private static final String BY_USER_QUERY = "SELECT DISTINCT P.* FROM " + TABLE_NAME + " P "
			+ "INNER JOIN " + RolePermissionRepository.TABLE_NAME + " RP ON(RP.PERMISSION_ID = P.ID) "
			+ "INNER JOIN " + UserRoleRepository.TABLE_NAME + " UR ON(UR.ROLE_ID = RP.ROLE_ID) "
			+ "WHERE UR.USER_ID = ?";
	
	
	private PreparedStatement byRole;
	private PreparedStatement byUser;

	public PermissionRepository(Connection connection, IMapResultSetToEntity<Permission> mapper, IUnitOfWork uow) {
		super(connection, mapper, uow);
		/*
		 * By mozna bylo wykonywac w prosty sposob dodatkowe operacje wyszukiwania utworzone zostaja 2 dodatkowe pytania sql.
		 */
		try {
			byRole = connection.prepareStatement(BY_ROLE_QUERY);
			byUser = connection.prepareStatement(BY_USER_QUERY);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setUpdateQuery(Permission p) throws SQLException {
		update.setString(1, p.getName());
		update.setInt(2, p.getId());
	}

	@Override
	protected void setInsertQuery(Permission p) throws SQLException {
		insert.setString(1, p.getName());
	}

	@Override
	protected String tableName() {
		return TABLE_NAME;
	}

	@Override
	protected String createTableSql() {
		return CREATE_TABLE_QUERY;
	}

	@Override
	protected String insertSql() {
		return INSERT_QUERY;
	}

	@Override
	protected String updateSql() {
		return UPDATE_QUERY;
	}

	/*
	 * 1. Utworz wynikowa liste
	 * 2. Wyciagnij z bazy Permissiony dla podanej roli
	 * 3. Podczas zapisywania do wynikowej listy do razu dodawaj role do permissiona by utworzyc relacje w runtimie
	 * 4. Dodaj wynikowa liste permissionow do roli by utworzyc relacje w runtimie
	 * 5. zwroc wynik
	 */
	@Override
	public List<Permission> byRole(Role role) {
		try {
			List<Permission> permissions = new ArrayList<>();
			
			byRole.setInt(1, role.getId());
			ResultSet rs = byRole.executeQuery();
			
			while(rs.next()) {
				Permission permission = mapper.map(rs);
				permission.getRoles().add(role);
				permissions.add(permission);
			}
			
			role.getPermissions().addAll(permissions);
			return permissions;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Jak wyzej tylko zamist roli jest user.
	 * Poniewaz permission nie zawiera wprost usera to dodawanie usera do permissiona nie jest mozliwe do wykonania
	 * Ale na koniec lista permissionow jest dodawana do usera by utworzyc relacje w runtimie
	 */
	@Override
	public List<Permission> byUser(User user) {
		try {
			List<Permission> permissions = new ArrayList<>();
			
			byUser.setInt(1, user.getId());
			ResultSet rs = byUser.executeQuery();
			
			while(rs.next()) {
				Permission permission = mapper.map(rs);
				permissions.add(permission);
			}
			
			user.getPermissions().addAll(permissions);
			return permissions;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

}