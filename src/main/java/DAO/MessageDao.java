package DAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import Model.Message;
import Util.ConnectionUtil;

public class MessageDao implements BaseDao<Message>
{
  private static final Logger LOGGER=LoggerFactory.getLogger(MessageDao.class);

    private void handleSQLException(SQLException e,String sql,String errorMessage)
    {
        LOGGER.error("SQLException: {},SQL: {}",e.getMessage(),sql);
        throw new DaoException(errorMessage,e);
    }
    public Optional<Message>getById(int id)
    {
        return executeQuery("select * from message where message_id=?",ps->ps.setInt(1,id))
                .stream().findFirst();
    }
    public List<Message>getAll()
    {
        return executeQuery("select * from message",ps->{});
    }

    public List<Message>getMessagesByAccountId(int accountId)
    {
        return executeQuery("select * from message where posted_by=?",ps->ps.setInt(1,accountId));
    }
    public Message insert(Message message)
    {
        String sql="insert into message(posted_by,message_text,time_posted_epoch)values(?,?,?)";
        try (Connection conn=ConnectionUtil.getConnection();
             PreparedStatement ps=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS))
            {
            ps.setInt(1,message.getPosted_by());
            ps.setString(2,message.getMessage_text());
            ps.setLong(3,message.getTime_posted_epoch());
            ps.executeUpdate();
            try (ResultSet keys=ps.getGeneratedKeys())
            {
                if(keys.next()) return new Message(keys.getInt(1),message.getPosted_by(),message.getMessage_text(),message.getTime_posted_epoch());
            }
        }catch(SQLException e)
        {
            handleSQLException(e,sql,"Error inserting message");
        }
        throw new DaoException("Failed to insert message");
    }
    public boolean update(Message message)
    {
        return executeUpdate("update message set posted_by=?,message_text=?,time_posted_epoch=? where message_id=?",
                ps->{
                    ps.setInt(1,message.getPosted_by());
                    ps.setString(2,message.getMessage_text());
                    ps.setLong(3,message.getTime_posted_epoch());
                    ps.setInt(4,message.getMessage_id());
                });
    }
    public boolean delete(Message message)
    {
        return executeUpdate("delete from message where message_id=?",ps->ps.setInt(1, message.getMessage_id()));
    }
    private List<Message> executeQuery(String sql,SQLConsumer<PreparedStatement> paramSetter)
    {
        List<Message> messages=new ArrayList<>();
        try (Connection conn=ConnectionUtil.getConnection();
             PreparedStatement ps=conn.prepareStatement(sql))
            {
            paramSetter.accept(ps);
            try (ResultSet rs=ps.executeQuery())
            {
                while (rs.next()) messages.add(mapResultSetToMessage(rs));
            }
        }catch(SQLException e)
        {
            handleSQLException(e,sql,"Error executing query");
        }
        return messages;
    }
    private boolean executeUpdate(String sql,SQLConsumer<PreparedStatement>paramSetter)
    {
        try (Connection conn=ConnectionUtil.getConnection();
             PreparedStatement ps=conn.prepareStatement(sql))
        {
            paramSetter.accept(ps);
            return ps.executeUpdate()>0;
        }catch (SQLException e)
        {
            handleSQLException(e,sql,"Error executing update");
        }
        return false;
    }
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException
    {
        return new Message(rs.getInt("message_id"),rs.getInt("posted_by"),
                rs.getString("message_text"),rs.getLong("time_posted_epoch"));
    }
    private interface SQLConsumer<T>
    {
        void accept(T t) throws SQLException;
    }
}

