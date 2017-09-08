package mahopam;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * This sample shows how to create a Lambda function for handling Alexa Skill requests that:
 *
 * <ul>
 * <li><b>Custom slot type</b>: demonstrates using custom slot types to handle a finite set of known values</li>
 * </ul>
 * <p>
 * <h2>Examples</h2>
 * <p>
 * <b>One-shot model</b>
 * <p>
 * User: "Alexa, ask Minecraft Helper how to make paper."
 * <p>
 * Alexa:"(reads back recipe for paper)."
 */
public class AlexaTimerSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(AlexaTimerSpeechlet.class);

    /**
     * The key to get the item from the intent.
     */
    private static final String TASK_SLOT = "Task";
    private static final String DATE_SLOT = "Date";

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        String speechOutput =
                "Welcome to your personal alexa time tracking. You can start or stop a task like, "
                        + "start task making bacon pancakes ... Now, what can I help you with?";
        // If the user either does not reply to the welcome message or says
        // something that is not understood, they will be prompted again with this text.
        String repromptText = "For instructions on what you can say, please say help me.";

        // Here we are prompting the user for input
        return newAskResponse(speechOutput, repromptText);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("StartTaskIntent".equals(intentName)) {
            return startTask(intent, session);
        } else if ("StopAllTasksIntent".equals(intentName)) {
            return stopAllTasks(intent, session);
        } else if ("StopLastTaskIntent".equals(intentName)) {
            return stopLastTask(intent, session);
        } else if ("StopTaskIntent".equals(intentName)) {
            return stopTask(intent, session);
        } else if ("GetCurrentTaskIntent".equals(intentName)) {
            return getCurrentTask(intent, session);
        } else if ("GetReportIntent".equals(intentName)) {
            return getReport(intent, session);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelp();
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any cleanup logic goes here
    }

    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse startTask(Intent intent, Session session) {
        Slot taskSlot = intent.getSlot(TASK_SLOT);
        if (taskSlot != null && taskSlot.getValue() != null) {
            String taskName = taskSlot.getValue();

            //Code for starttask
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Task " + taskName + " initiated.");

            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();  
            String startDate = df.format(today);

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;

	    try {
		conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("INSERT INTO task_entry (user_id, start_time, task_name) VALUES (?, NOW(), ?)");

		stmt.setString(1, session.getUser().getUserId());
		stmt.setString(2, taskName);

		stmt.executeUpdate();

		//conn.commit();

	        card.setTitle("Task " + taskName + " initiated.");
        	card.setContent("Task was started at " + startDate);

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Task " + taskName + " could not be initiated.");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setText("Error initiating task " + taskName);
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
        } else {
            // There was no item in the intent so return the help prompt.
            return getHelp();
        }
    }

    private Connection getRdsConnection() throws SQLException
    {
        return DriverManager.getConnection(System.getenv("rds_connection_string"));
    }
    
    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse stopTask(Intent intent, Session session) {
        Slot taskSlot = intent.getSlot(TASK_SLOT);
        if (taskSlot != null && taskSlot.getValue() != null) {
            String taskName = taskSlot.getValue();

            //Code for starttask
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Task " + taskName + " stopped.");

            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();  
            String startDate = df.format(today);

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;

	    try {
		conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("UPDATE task_entry SET end_time = NOW() WHERE user_id = ? AND task_name LIKE ?");

		stmt.setString(1, session.getUser().getUserId());
		stmt.setString(2, taskName);

		stmt.executeUpdate();

		//conn.commit();

	        card.setTitle("Task " + taskName + " stopped.");
        	card.setContent("Task was stopped at " + startDate);

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Task " + taskName + " could not be initiated.");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setText("Error stoping task " + taskName);
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
        } else {
            // There was no item in the intent so return the help prompt.
            return getHelp();
        }
    }


    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getReport(Intent intent, Session session) {
            Slot dateSlot = intent.getSlot(DATE_SLOT);
            if (dateSlot == null || dateSlot.getValue() == null) {
                return getHelp();
            }

            SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;
            String dateISO = dateSlot.getValue();

	    try {
                conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("SELECT task_name, TIMESTAMPDIFF(MINUTE,start_time,end_time) AS total_time FROM task_entry WHERE user_id = ? AND end_time IS NOT NULL AND start_time BETWEEN ? AND ? ORDER BY start_time DESC LIMIT 1");

		stmt.setString(1, session.getUser().getUserId());
		stmt.setString(2, dateISO + " 00:00:00");
		stmt.setString(3, dateISO + " 23:59:59");

		rs = stmt.executeQuery();

                outputSpeech.setSsml("<speak>You don't have any recorded task for that day<speak>");
		card.setTitle("You don't have any recorded task for "+dateISO);

                String report = "";

		while(rs.next()) {
		    String taskName = rs.getString("task_name");
		    String totalTime = rs.getString("total_time");

                    report += "<s>Task " + taskName + "<break time='1s' />, recorded time " 
                            + "  <say-as interpret-as='cardinal'>" + totalTime + "</say-as> minutes</s>";
		}
                 
                if (!report.toString().isEmpty()) {
                    report = "<speak>This is the report of the date <say-as interpret-as='date'>"+dateISO+"</say-as><break time='1s' />" + report + "</speak>";
                    outputSpeech.setSsml(report);
	            card.setTitle("Report of the date " + dateISO);
           	    card.setContent(report); //FIXME
                }

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Report could not be retrieved");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setSsml("<speak>Error retrieving tasks report<speak>");
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
    }


    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getCurrentTask(Intent intent, Session session) {
            //Code for starttask
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;

	    try {
                conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("SELECT task_name, start_time FROM task_entry WHERE user_id = ? AND end_time IS NULL ORDER BY start_time DESC LIMIT 1");

		stmt.setString(1, session.getUser().getUserId());

		rs = stmt.executeQuery();

                outputSpeech.setText("You don't have any active task");
		card.setTitle("You don't have any active task");

		while(rs.next()) {
		   String taskName = rs.getString("task_name");
		   String startTime = rs.getString("start_time");
                   outputSpeech.setText("The current task is "+taskName);
	           card.setTitle("Your active task is " + taskName);
           	   card.setContent("Started at " + startTime);
		}

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Tasks could not be retrieved");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setText("Error retrieving active tasks");
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
    }
    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse stopLastTask(Intent intent, Session session) {
            //Code for starttask
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Last task stopped");

            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();
            String startDate = df.format(today);

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;

	    try {
                conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("UPDATE task_entry SET end_time = NOW() WHERE user_id = ? AND end_time IS NULL ORDER BY start_time DESC LIMIT 1");

		stmt.setString(1, session.getUser().getUserId());

		stmt.executeUpdate();

		//conn.commit();

	        card.setTitle("All tasks stopped");
        	card.setContent("Tasks stopped at " + startDate);

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Tasks could not be stopped.");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setText("Error stoping tasks");
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the RecipeIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse stopAllTasks(Intent intent, Session session) {
            //Code for starttask
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("All tasks stopped");

            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();
            String startDate = df.format(today);

            SimpleCard card = new SimpleCard();
	    PreparedStatement stmt = null;
   	    ResultSet rs = null;
 	    Connection conn = null;

	    try {
                conn = getRdsConnection();
		conn.setAutoCommit(true);
		stmt = conn.prepareStatement("UPDATE task_entry SET end_time = NOW() WHERE user_id = ? AND end_time IS NULL");

		stmt.setString(1, session.getUser().getUserId());

		stmt.executeUpdate();

		//conn.commit();

	        card.setTitle("All tasks stopped");
        	card.setContent("Tasks stopped at " + startDate);

		stmt.close();

  	    } catch (SQLException ex) {
	        card.setTitle("Tasks could not be stopped.");
        	card.setContent("Error: " + ex.getMessage());
		outputSpeech.setText("Error stoping tasks");
	    }

            return SpeechletResponse.newTellResponse(outputSpeech, card);
    }
 
    /**
     * Creates a {@code SpeechletResponse} for the HelpIntent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelp() {
        String speechOutput =
                "You can start a task with the phrase, Start task making bacon pancakes, "
                        + "or, you can say stop current task... "
                        + "Now, what can I help you with?";
        String repromptText =
                "You can say things like, stop task development phase"
                        + " , or you can say stop all tasks... Now, what can I help you with?";
        return newAskResponse(speechOutput, repromptText);
    }

    /**
     * Wrapper for creating the Ask response. The OutputSpeech and {@link Reprompt} objects are
     * created from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(stringOutput);

        PlainTextOutputSpeech repromptOutputSpeech = new PlainTextOutputSpeech();
        repromptOutputSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);

        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}
