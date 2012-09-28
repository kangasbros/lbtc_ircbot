import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.jdom.JDOMException;

import java.util.Iterator;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import org.jdom.Document;

import org.pircbotx.PircBotX;

public class IRCBot {
	static Timer timer = new Timer ();

	static PircBotX bot1 = new PircBotX();
	static PircBotX bot2 = new PircBotX();
	static int timerdelay = 0;
	static String ircnetch = "bitcoins.fi"; // IRCNETIN KANNUN NIMI

	static String[] channels;

	static TimerTask hourlyTask = new TimerTask () {
		@Override
		public void run() {
			System.out.println("Tarkistusta aloitetaan..."); // TARKISTUKSEN ALOITUS
			for (int u = 0; u < channels.length;) { // TASSA KAYDAAN LAPI JOKAINEN KANAVA channels-arrayssa
				try {
					String channel = channels[u];
					String feedend = channels[u+1];
					URL url = new URL("https://localbitcoins.com/country_rss/" + feedend);

					XmlReader reader = null;
					XmlReader reader2 = null;

					ArrayList dates = new ArrayList();
					Date founddate = null;

					reader = new XmlReader(url);
					reader2 = new XmlReader(url);

					SyndFeed feed = new SyndFeedInput().build(reader);
					SyndFeed feed2 = new SyndFeedInput().build(reader2);

					Calendar cal = Calendar.getInstance();
					DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
					Date now = formatter.parse(formatter.format(cal.getTime()));

					int a = 0;
					for (Iterator i = feed.getEntries().iterator(); i.hasNext();) { // LISATAAN JOKAINEN MERKINTA TAULUUN
						SyndEntry entry = (SyndEntry) i.next();
						Date pubdate2 = entry.getPublishedDate();
						Date pubdate = formatter.parse(formatter.format(pubdate2));
						dates.add(pubdate);
						a++;
					}
					Collections.sort(dates); // JARJESTETAAN PAIVAMAARAT VANHIMMISTA UUSIMPAAN
					for(Iterator<Date> i = dates.iterator(); i.hasNext(); ) {
						Date date = i.next();
						long result = Math.abs(now.getTime() - date.getTime());
						if (Math.abs(now.getTime() - date.getTime()) <= timerdelay) { //TARKISTETAAN JOS PAIVAMAARA ON UPDATEINTERVALIN SISALLA
							System.out.println("Uusi ilmoitus loydetty!");
							founddate = date;
                                                        break;
						}
					}
					for (Iterator t = feed2.getEntries().iterator(); t.hasNext();) { // JA LAHETATAAN VIESTIT...
						SyndEntry entry = (SyndEntry) t.next();
						Date pubdate2 = entry.getPublishedDate();
						Date pubdate = formatter.parse(formatter.format(pubdate2));
						if (founddate != null && Math.abs(pubdate.getTime() - founddate.getTime()) == 0) {
							String title = entry.getTitle();
							String message = "";
							String desc = entry.getDescription().getValue();
							String link = entry.getLink();
							String guid = entry.getGuid();
							message = "";
							if (title.startsWith("Buy")) {
								if (channel == ircnetch) {
									message = "Myy bitcoineja: " + desc + ". " + guid;
								} else {
									message = "Sell your bitcoins locally: " +  desc + ". " + guid;
								}
							} else if (title.startsWith("Sell")) {
								if (channel == ircnetch) {
									message = "Osta bitcoineja käteisellä: " + desc + ". " + guid;
								} else {
									message = "Buy bitcoins with cash: " +  desc + ". " + guid;
								}
							}
							System.out.println("Lahetetaan ilmoitusta kanavalle...");
							if (channel == ircnetch) {
								bot1.sendMessage("#" + channel, message);
							} else {
								bot2.sendMessage("#" + channel, message);
							}

						}
					}

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FeedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Tarkistus lopetettu.");
				u++;
				u++;
			}

		}

	};

	public static void main(String[] args) throws Exception {
		// PARAMETERS: NICK DEBUG TIMERDELAY [FREENODECHANNEL, FEEDEND]
		String nick = args[0]; // NICK
		boolean verbose = Boolean.parseBoolean(args[1]); // CONVERT STRING TO BOOLEAN
		timerdelay = Integer.parseInt(args[2]);  // UPDATEDELAY
		channels = new String[args.length - 1];
		int channelsum = 0;
		for (int i = 3; i < args.length; i++) {  // IMPORT CHANNEL/FEED PARAMETERS INTO AN STATIC ARRAY
			channels[channelsum] = args[i];
			channelsum++;
		}

		channels[channelsum] = ircnetch; // LISÄTÄÄN SUOMEN KANNU SYKLIIN JOTTA SINNEKIN ILMOITETAAN
		channels[channelsum + 1] = "FI";
		System.out.println("Connecting to channels... with nickname " + nick + ".");
		bot1.setName(nick);
		bot1.connect("irc.swipnet.se");
		bot1.setVerbose(verbose);
		bot1.joinChannel("#" + ircnetch);
		
		bot2.setName(nick);
		bot2.connect("irc.freenode.net");
		bot2.setVerbose(verbose);
		for (int a = 0; a < channels.length;) {
			if (channels[a] != ircnetch) {
			bot2.joinChannel("#" + channels[a]);
			}
			a++;a++;
		}

		timer.schedule(hourlyTask, 10000, timerdelay);
	}
}
