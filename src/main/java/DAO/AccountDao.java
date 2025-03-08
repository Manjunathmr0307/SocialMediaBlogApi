package DAO;
import Model.Account;
import Util.ConnectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.*;

public class AccountDao implements BaseDao<Account>
{
    private static final Logger LOGGER=LoggerFactory.getLogger(AccountDao.class);

    private void handleSQLException(SQLException e,String sql,String message)
    {
        LOGGER.error("{} | SQL: {} | Error: {}",message,sql,e.getMessage());
        throw new DaoException(message,e);
    }
    private Optional<Account> mapAccount(ResultSet rs) throws SQLException 
    {
        return rs.next()?Optional.of(new Account(rs.getInt("account_id"),rs.getString("username"),rs.getString("password"))):Optional.empty();
    }
    public Optional<Account> getById(int id)
    {
        String sql="select * from account where account_id=?";
        try(PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql))
        {
            ps.setInt(1,id);
            return mapAccount(ps.executeQuery());
        }catch(SQLException e) 
        {
            handleSQLException(e,sql,"Error retrieving account by ID");
        }
        return Optional.empty();
    }
    public List<Account> getAll()
    {
        List<Account> accounts=new ArrayList<>();
        String sql="select * from account";
        try (PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql);
             ResultSet rs=ps.executeQuery()) 
        {
            while (rs.next())accounts.add(new Account(rs.getInt("account_id"),rs.getString("username"),rs.getString("password")));
        } 
        catch(SQLException e)
        {
            handleSQLException(e,sql,"Error retrieving all accounts");
        }
        return accounts;
    }
    public Optional<Account> findAccountByUsername(String username)
    {
        String sql="select * from account where username=?";
        try(PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql))
        {
            ps.setString(1,username);
            return mapAccount(ps.executeQuery());
        }catch(SQLException e)
        {
            handleSQLException(e,sql,"Error finding account by username");
        }
        return Optional.empty();
    }
    public Optional<Account> validateLogin(String username, String password)
    {
        return findAccountByUsername(username).filter(acc->acc.getPassword().equals(password));
    }
    public boolean doesUsernameExist(String username)
    {
        String sql="select count(*)from account where username=?";
        try (PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql))
        {
            ps.setString(1,username);
            try (ResultSet rs=ps.executeQuery())
            {
                return rs.next()&&rs.getInt(1)>0;
            }
        }catch (SQLException e)
        {
            handleSQLException(e,sql,"Error checking username existence");
        }
        return false;
    }
    public Account insert(Account account)
    {
        String sql="insert into account(username,password)values(?,?)";
        try (PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql,Statement.RETURN_GENERATED_KEYS))
        {
            ps.setString(1,account.getUsername());
            ps.setString(2,account.getPassword());
            ps.executeUpdate();
            try (ResultSet keys=ps.getGeneratedKeys())
            {
                if (keys.next()) return new Account(keys.getInt(1),account.getUsername(),account.getPassword());
            }
        }catch(SQLException e)
        {
            handleSQLException(e,sql,"Error inserting account");
        }
        throw new DaoException("Creating account failed,no ID obtained.");
    }
    public boolean update(Account account)
    {
        String sql="update account set username=?,password=? where account_id=?";
        try(PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql))
        {
            ps.setString(1,account.getUsername());
            ps.setString(2,account.getPassword());
            ps.setInt(3, account.getAccount_id());
            return ps.executeUpdate()>0;
        }catch(SQLException e)
        {
            handleSQLException(e,sql,"Error updating account");
        }
        return false;
    }
    public boolean delete(Account account)
    {
        String sql ="delete from account where account_id=?";
        try (PreparedStatement ps=ConnectionUtil.getConnection().prepareStatement(sql))
        {
            ps.setInt(1,account.getAccount_id());
            return ps.executeUpdate()>0;
        }catch(SQLException e) {
            handleSQLException(e,sql,"Error deleting account");
        }
        return false;
    }
}

