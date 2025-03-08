package Service;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import DAO.MessageDao;
import DAO.DaoException;
import Model.Account;
import Model.Message;
import io.javalin.http.NotFoundResponse;

public class MessageService
{
    private final MessageDao messageDao;
    private static final Logger LOGGER=LoggerFactory.getLogger(MessageService.class);
    private static final String DB_ACCESS_ERROR_MSG="Error accessing the database";

    public MessageService()
    {
        this(new MessageDao());
    }
    public MessageService(MessageDao messageDao)
    {
        this.messageDao=messageDao;
    }
    public Optional<Message> getMessageById(int id)
    {
        LOGGER.info("Fetching message with ID: {}",id);
        return execute(()->messageDao.getById(id),"fetching message");
    }
    public List<Message> getAllMessages()
    {
        LOGGER.info("Fetching all messages");
        return execute(messageDao::getAll,"fetching messages");
    }
    public List<Message> getMessagesByAccountId(int accountId)
    {
        LOGGER.info("Fetching messages for account ID: {}", accountId);
        return execute(()->messageDao.getMessagesByAccountId(accountId),"fetching messages by account ID");
    }
    public Message createMessage(Message message,Optional<Account> account)
    {
        LOGGER.info("Creating message: {}",message);
        account.orElseThrow(()->new ServiceException("Account must exist when posting a message"));
        validateMessage(message);
        checkAccountPermission(account.get(),message.getPosted_by());
        return execute(()->messageDao.insert(message),"creating message");
    }
    public Message updateMessage(Message message)
    {
        LOGGER.info("Updating message ID: {}", message.getMessage_id());
        Message existingMessage=getMessageById(message.getMessage_id()).orElseThrow(()->new ServiceException("Message not found"));
        existingMessage.setMessage_text(message.getMessage_text());
        validateMessage(existingMessage);
        return execute(()->{
            messageDao.update(existingMessage);
            return existingMessage;
        },"updating message");
    }
    public void deleteMessage(Message message)
    {
        LOGGER.info("Deleting message: {}",message);
        execute(()->{
            if(!messageDao.delete(message))
            {
                throw new NotFoundResponse("Message to delete not found");
            }
            return null;
        },"deleting message");
    }
    private void validateMessage(Message message)
    {
        LOGGER.info("Validating message: {}",message);
        String text=message.getMessage_text();
        if (text==null||text.trim().isEmpty())
        {
            throw new ServiceException("Message text cannot be empty");
        }
        if (text.length()>254)
        {
            throw new ServiceException("Message text cannot exceed 254 characters");
        }
    }
    private void checkAccountPermission(Account account, int postedBy)
    {
        LOGGER.info("Checking permissions for account ID: {}",account.getAccount_id());
        if (account.getAccount_id()!=postedBy)
        {
            throw new ServiceException("Account not authorized to modify this message");
        }
    }
    private <T> T execute(ExceptionSupplier<T> action,String errorMessage)
    {
        try {
            return action.get();
        }catch(DaoException e)
        {
            throw new ServiceException(DB_ACCESS_ERROR_MSG,e);
        }
    }
    private interface ExceptionSupplier<T>
    {
        T get() throws DaoException;
    }
}
