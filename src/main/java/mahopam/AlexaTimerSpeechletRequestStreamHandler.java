package mahopam;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

/**
 * This class is created by the Lambda environment when a request comes in. All calls will be
 * dispatched to the Speechlet passed into the super constructor.
 */
public final class AlexaTimerSpeechletRequestStreamHandler extends
        SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds;

    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.5284f609-932f-42f1-aa26-c012411e930b");
    }

    public AlexaTimerSpeechletRequestStreamHandler() {
        super(new AlexaTimerSpeechlet(), supportedApplicationIds);
    }
}
