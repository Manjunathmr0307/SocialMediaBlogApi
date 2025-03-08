package Controller;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import Model.Account;
import Model.Message;
import Service.AccountService;
import Service.MessageService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class SocialMediaController
{
    private final AccountService accountService=new AccountService();
    private final MessageService messageService=new MessageService();
    private final ObjectMapper mapper=new ObjectMapper();

    public Javalin startAPI() {
        Javalin app=Javalin.create();
        app.post("/register",this::registerAccount);
        app.post("/login",this::loginAccount);
        app.post("/messages",this::createMessage);
        app.get("/messages",ctx->ctx.json(messageService.getAllMessages()));
        app.get("/messages/{message_id}",this::getMessageById);
        app.delete("/messages/{message_id}",this::deleteMessageById);
        app.patch("/messages/{message_id}",this::updateMessageById);
        app.get("/accounts/{account_id}/messages",this::getMessagesByAccountId);
        return app;
    }
    private void registerAccount(Context ctx)
    {
        try
        {
            Account account=mapper.readValue(ctx.body(),Account.class);
            ctx.json(mapper.writeValueAsString(accountService.createAccount(account)));
        }catch(Exception e)
        {
            ctx.status(400);
        }
    }
    private void loginAccount(Context ctx)
    {
        try
        {
            Account account=mapper.readValue(ctx.body(), Account.class);
            Optional<Account> loggedInAccount=accountService.validateLogin(account);
            loggedInAccount.ifPresentOrElse(ctx::json,()->ctx.status(401));
        }catch(Exception e)
        {
            ctx.status(401);
        }
    }
    private void createMessage(Context ctx)
    {
        try
        {
            Message message=mapper.readValue(ctx.body(), Message.class);
            Optional<Account> account=accountService.getAccountById(message.getPosted_by());
            ctx.json(messageService.createMessage(message,account));
        }catch(Exception e)
        {
            ctx.status(400);
        }
    }
    private void getMessageById(Context ctx)
    {
        try 
        {
            int id=Integer.parseInt(ctx.pathParam("message_id"));
            messageService.getMessageById(id).ifPresentOrElse(ctx::json, ()->ctx.status(200).result(""));
        }catch(NumberFormatException e)
        {
            ctx.status(400);
        }
    }
    private void deleteMessageById(Context ctx)
    {
        try
        {
            int id=Integer.parseInt(ctx.pathParam("message_id"));
            messageService.getMessageById(id).ifPresent(message->{
                messageService.deleteMessage(message);
                ctx.json(message);
            });
            ctx.status(200);
        }catch(Exception e)
        {
            ctx.status(200);
        }
    }
    private void updateMessageById(Context ctx)
    {
        try 
        {
            int id=Integer.parseInt(ctx.pathParam("message_id"));
            Message message=mapper.readValue(ctx.body(),Message.class);
            message.setMessage_id(id);
            ctx.json(messageService.updateMessage(message));
        }catch(Exception e)
        {
            ctx.status(400);
        }
    }
    private void getMessagesByAccountId(Context ctx)
    {
        try
        {
            int accountId=Integer.parseInt(ctx.pathParam("account_id"));
            List<Message> messages=messageService.getMessagesByAccountId(accountId);
            ctx.json(messages);
        }catch(Exception e)
        {
            ctx.status(400);
        }
    }
}
