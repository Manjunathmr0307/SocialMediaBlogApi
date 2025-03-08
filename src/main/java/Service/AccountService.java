package Service;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import DAO.AccountDao;
import DAO.DaoException;
import Model.Account;

public class AccountService
{
    private final AccountDao accountDao;
    private static final Logger LOGGER=LoggerFactory.getLogger(AccountService.class);

    public AccountService()
    {
        this(new AccountDao());
    }
    public AccountService(AccountDao accountDao)
    {
        this.accountDao=accountDao;
    }
    public Optional<Account>getAccountById(int id)
    {
        LOGGER.info("Fetching account with ID: {}",id);
        return execute(()->accountDao.getById(id),"fetching account");
    }
    public List<Account>getAllAccounts()
    {
        LOGGER.info("Fetching all accounts");
        return execute(accountDao::getAll,"fetching accounts");
    }
    public Optional<Account>findAccountByUsername(String username)
    {
        LOGGER.info("Finding account by username: {}",username);
        return execute(()->accountDao.findAccountByUsername(username), 
                "finding account by username " + username);
    }
    public Optional<Account>validateLogin(Account account)
    {
        LOGGER.info("Validating login for username: {}",account.getUsername());
        return execute(() -> accountDao.validateLogin(account.getUsername(),account.getPassword()), 
                "validating login");
    }
    public Account createAccount(Account account)
    {
        LOGGER.info("Creating account: {}", account);
        validateAccount(account);
        if (findAccountByUsername(account.getUsername()).isPresent())
        {
            throw new ServiceException("Account already exists");
        }
        return execute(()->accountDao.insert(account),"creating account");
    }
    public boolean updateAccount(Account account)
    {
        LOGGER.info("Updating account: {}",account);
        return execute(()->accountDao.update(account),"updating account");
    }
    public boolean deleteAccount(Account account) 
    {
        LOGGER.info("Deleting account: {}",account);
        if (account.getAccount_id()==0)
        {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        return execute(()->accountDao.delete(account),"deleting account");
    }
    private void validateAccount(Account account)
    {
        LOGGER.info("Validating account: {}",account);
        String username=account.getUsername().trim();
        String password=account.getPassword().trim();

        if(username.isEmpty()||password.isEmpty())
        {
            throw new ServiceException("Username and password cannot be blank");
        }
        if(password.length()<4)
        {
            throw new ServiceException("Password must be at least 4 characters long");
        }
        if(accountDao.doesUsernameExist(username))
        {
            throw new ServiceException("The username must be unique");
        }
    }
    public boolean accountExists(int accountId)
    {
        LOGGER.info("Checking if account exists with ID: {}",accountId);
        return getAccountById(accountId).isPresent();
    }
    private <T> T execute(ExceptionSupplier<T> action,String errorMessage)
    {
        try
        {
            return action.get();
        }catch(DaoException e)
        {
            throw new ServiceException("Exception occurred while " + errorMessage,e);
        }
    }
    private interface ExceptionSupplier<T>
    {
        T get() throws DaoException;
    }
}
