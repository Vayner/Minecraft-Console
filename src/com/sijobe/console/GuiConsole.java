package net.minecraft.src;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * TODO: P1 - Only save logs for the current world
 * TODO: P1 - Input/output filtering
 * TODO: P3 - Text selection
 * TODO: P2 - Spinner (tab auto complete)
 * TODO: P3 - Drop down menus
 * TODO: P2 - Improve look and feel
 * DONE: P1 - Username autocomplete (servers)
 * DONE: P1 - Key bindings (tick hook)
 * DONE: P1 - Validate clipboard string has valid characters
 * 
 * @author simo_415
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
public class GuiConsole extends GuiScreen implements Runnable {

   protected String message;
   private int updateCounter;
   private int slider;
   private int cursor;
   private int inputOffset;
   private int sliderHeight;
   private boolean isSliding;
   private int lastSliding;
   private int initialSliding;
   private int historyPosition;
   private boolean isGuiOpen;
   private ChatLine lastOutput;
   private boolean rebuildLines;
   private volatile Vector<String> log;
   private SimpleDateFormat sdf;
   
   private int tabPosition;
   private String tabWord;
   
   private volatile HashMap<String,String> keyBindings;
   private volatile List<Integer> keyDown;
   private static boolean BACKGROUND_BINDING_EVENTS = false;

   private String logName;
   private long lastWrite;

   private static final String ALLOWED_CHARACTERS;

   private static int MESSAGE_MAXX;
   private static int MESSAGE_MINX;

   public static Vector<String> INPUT_HISTORY;
   private static Vector<String> LINES;
   private static Vector<String> MESSAGES;
   private static Vector<ConsoleListener> LISTENERS;

   private static volatile List IN_GAME_GUI;
   private static List IN_GAME_GUI_TEMP;

   private static int[] TOP;
   private static int[] BOTTOM;
   private static int[] BAR;
   private static int[] EXIT;

   // Character height - used to quickly determine number of lines per view
   private static int CHARHEIGHT = 10;
   // Maximum character width - used to quickly determine line length
   private static double CHARWIDTH = 6;

   private static boolean CLOSE_ON_SUBMIT = false;

   private static int INPUT_MAX = 100;
   private static int INPUT_HISTORY_MAX = 50;
   private static String INPUT_PREFIX = "> ";

   private static boolean PRINT_INPUT = true;
   private static boolean PRINT_OUTPUT = true;

   private static boolean HISTORY_BETWEEN_WORLDS = true;

   public static final int LOGGING_TRACE = 8;
   public static final int LOGGING_DEBUG = 4;
   public static final int LOGGING_INPUT = 2;
   public static final int LOGGING_OUTPUT = 1;
   private static int LOGGING = LOGGING_INPUT + LOGGING_OUTPUT;
   private static long LOG_WRITE_INTERVAL = 1000L;
   private static String LINE_BREAK = System.getProperty("line.separator");

   private static String DATE_FORMAT_LOG = "yyyy-MM-dd hh:mm:ss: ";
   private static String DATE_FORMAT_FILENAME = "yyyyMMdd_hhmmss'.log'";

   private static int OUTPUT_MAX = 200;

   private static long POLL_DELAY = 20L;

   private static int BORDERSIZE = 2;
   private static int SCREEN_PADDING_LEFT = 5;
   private static int SCREEN_PADDING_TOP = 12;
   private static int SCREEN_PADDING_RIGHT = 5;
   private static int SCREEN_PADDING_BOTTOM = 40;

   private static int COLOR_BASE = 0x90000000;
   private static int COLOR_SCROLL_BACKGROUND = 0xBB999999;
   private static int COLOR_SCROLL_FOREGROUND = 0xBB404040;
   private static int COLOR_INPUT_TEXT = 0xE0E0E0;
   private static int COLOR_TEXT_OUTPUT = 0xE0E0E0;
   private static int COLOR_TEXT_TITLE = 0xE0E0E0;
   private static int COLOR_SCROLL_ARROW = 0xFFFFFF;
   private static int COLOR_EXIT_BUTTON_TEXT = 0xFFFFFF;
   private static int COLOR_EXIT_BUTTON = 0xBB999999;
   private static int COLOR_OUTPUT_BACKGROUND = 0xBB999999;
   private static int COLOR_INPUT_BACKGROUND = 0xBB999999;

   public static final String VERSION = "1.2";
   private static String TITLE = "Console v" + VERSION;

   private static final String MOD_PATH = "mods/console/";
   private static String LOG_PATH = "mods/console/logs";
   private static File MOD_DIR = new File(Minecraft.getMinecraftDir(),MOD_PATH);
   private static File LOG_DIR;

   private static GuiConsole INSTANCE;

   static {
      if ((new File(MOD_DIR,"gui.properties")).exists()) {
         readSettings(GuiConsole.class, new File(MOD_DIR,"gui.properties"));
      }
      writeSettings(GuiConsole.class, new File(MOD_DIR,"gui.properties"));
      LOG_DIR = new File(Minecraft.getMinecraftDir(),LOG_PATH);
      ALLOWED_CHARACTERS = ChatAllowedCharacters.allowedCharacters;
      MESSAGES = new Vector<String>();
      MESSAGES.add("\2476Minecraft Console version " + VERSION + " written by simo_415");
      MESSAGES.add("");
      INPUT_HISTORY = new Vector<String>();
      LISTENERS = new Vector<ConsoleListener>();
      TOP = new int[4];
      BOTTOM = new int[4];
      BAR = new int[4];
      EXIT = new int[4];
      INSTANCE = new GuiConsole();
      if (!MOD_DIR.exists()) {
         try {
            MOD_DIR.mkdirs();
         } catch (Exception e) {}
      }
      if (!LOG_DIR.exists()) {
         try {
            LOG_DIR.mkdirs();
         } catch (Exception e) {}
      }
      IN_GAME_GUI_TEMP = new Vector<Object>();
   }

   /**
    * Constructor should only be initialised from within the class
    */
   private GuiConsole() {
      (new Thread(this)).start();
      isGuiOpen = false;
      log = new Vector<String>();
      sdf = new SimpleDateFormat(DATE_FORMAT_LOG);
      keyBindings = generateKeyBindings();
      keyDown = new Vector<Integer>();
   }

   /**
    * SingleTon pattern to get an instance of the GUI
    * 
    * @return An instance of the GUI
    */
   public static GuiConsole getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new GuiConsole();
      }
      return INSTANCE;
   }

   /**
    * Generates a hashmap containing all of the configured key bindings from file
    * 
    * @return A hashmap containing all the keybindings
    */
   public HashMap<String,String> generateKeyBindings() {
      Properties p = new Properties();
      HashMap<String,String> bindings = new HashMap<String,String>();
      try {
         p.load(new FileInputStream(new File(MOD_DIR,"bindings.properties")));
         Iterator i = p.keySet().iterator();
         while (i.hasNext()) {
            String o = (String)i.next();
            bindings.put(o,(String)p.get(o));
         }
      } catch (Exception e) {}
      return bindings;
   }

   /**
    * Grabs the inGameGui chat list if it can be grabbed and returns true if
    * the list is not null
    * 
    * @return True is returned when the list is retrieved and not null
    */
   private boolean getInGameGuiList() {
      if (IN_GAME_GUI == null) {
         if (mc == null) {
            return false;
         }
         try {
            Field fields[] = GuiIngame.class.getDeclaredFields();
            for (Field f : fields) {
               f.setAccessible(true);
               Object o = f.get(mc.ingameGUI);
               if (o instanceof List) {
                  IN_GAME_GUI = (List<?>)o;
                  return true;
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
            return false;
         }
      } else {
         return true;
      }
      return false;
   }

   /**
    * Rebuilds the line list so that the text input and slider can be correctly
    * rendered without missing parts and dynamically resize if required.
    */
   public void buildLines() {
      LINES = new Vector<String>();
      for (String message : MESSAGES) {
         addLine(message);
      }
      rebuildLines = false;
   }

   /**
    * Adds a line to the line render list
    * 
    * @param message - The line message to add
    */
   public void addLine(String message) {
      if (LINES == null) {
         buildLines();
      }
      if (message == null) {
         return;
      }
      /*Vector<String> part = new Vector<String>();
      String tempMessage = message;
      do {
         if (fontRenderer.getStringWidth(tempMessage) < message_maxx - message_minx) {
            part.add(tempMessage);
            break;
         }
         int cut = 0;
         while (fontRenderer.getStringWidth(tempMessage.substring(0,tempMessage.length() - cut++)) >= message_maxx - message_minx);

         part.add(tempMessage.substring(0,tempMessage.length() - --cut));
         if (tempMessage.length() - ++cut >= tempMessage.length()) {
            break;
         }
         tempMessage = tempMessage.substring(tempMessage.length() - cut);
      } while (true);

      for (String line : part) {
         LINES.add(line);
      }*/

      /*if (message.startsWith("{")) {
         if (!PRINT_INPUT) {
            return;
         } else {
            if (message.length() > 1) {
               message = message.substring(1);
            }
         }
      } else if (message.startsWith("}")) {
         if (!PRINT_OUTPUT) {
            return;
         } else {
            if (message.length() > 1) {
               message = message.substring(1);
            }
         }
      }*/
      int chars = (int)((MESSAGE_MAXX - MESSAGE_MINX) / CHARWIDTH);
      String parts[] = message.split("(?<=\\G.{"+(chars)+"})");
      for (int i = 0; i < parts.length; i++) {
         LINES.add(parts[i]);
      }
   }

   /**
    * Called when Minecraft initialises the GUI
    * 
    * @see net.minecraft.src.GuiScreen#initGui()
    */
   @Override
   public void initGui() {
      Keyboard.enableRepeatEvents(true);
      isSliding = false;
      lastSliding = -1;
      slider = 0;
      initialSliding = 0;
      cursor = 0;
      message = "";
      updateCounter = 0;
      historyPosition = 0;
      isGuiOpen = true;
      rebuildLines = true;
      pullGuiList();
      if (getInGameGuiList()) {
         for (Object message : IN_GAME_GUI) {
            IN_GAME_GUI_TEMP.add(0,message);
         }
         IN_GAME_GUI.clear();
      }
   }

   /**
    * Called when the GUI is closed by Minecraft - useful for cleanup
    * 
    * @see net.minecraft.src.GuiScreen#onGuiClosed()
    */
   @Override
   public void onGuiClosed() {
      Keyboard.enableRepeatEvents(false);
      isGuiOpen = false;

      // Transfers the inGameGui messages back to the inGameGui object
      if (!isGuiOpen) {
         for (Object message : IN_GAME_GUI_TEMP) {
            IN_GAME_GUI.add(0,message);
         }
         IN_GAME_GUI_TEMP.clear();
      }
   }

   /**
    * Called to update the screen on frame
    * 
    * @see net.minecraft.src.GuiScreen#updateScreen()
    */
   @Override
   public void updateScreen() {
      updateCounter++;

      // Transfers the inGameGui messages from the inGameGui object to here
      pullGuiList();
      if (isGuiOpen) {
         for (int i = IN_GAME_GUI.size() - 1; i > -1; i--) {
            IN_GAME_GUI_TEMP.add(IN_GAME_GUI.get(i));
         }
         IN_GAME_GUI.clear();
      }
   }

   /**
    * Called when a key is typed, handles all input into the console
    * 
    * @see net.minecraft.src.GuiScreen#keyTyped(char, int)
    */
   @Override
   protected void keyTyped(char key, int id) {
      // Multi key validation
      // Control + ?
      if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
         if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            setClipboardString(message);
         } else if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
            String clipboard = getClipboardString();
            if (clipboard != null) {
               String start = "";
               String end = "";
               if (message != null && message.length() > 0) {
                  start = message.substring(0,cursor);
                  end = message.substring(cursor);
               }
               int limit = INPUT_MAX - message.length();
               if (limit < clipboard.length()) {
                  clipboard = clipboard.substring(0,limit);
               }
               message = start + clipboard + end;
               cursor = (start + clipboard).length() + 1;
            }
         }
         return;
      }
      
      if (id != Keyboard.KEY_TAB) {
         tabPosition = 0;
         tabWord = "";
      }

      // Single key validation
      switch (id) {
         case Keyboard.KEY_ESCAPE:
            // Exits the GUI
            mc.displayGuiScreen(null);
            break;

         case Keyboard.KEY_RETURN:
            // Submits the message
            String s = message.trim();
            if (s.length() > 0) {
               addInputMessage(s);
               mc.thePlayer.sendChatMessage(s);
            }
            if (CLOSE_ON_SUBMIT) {
               mc.displayGuiScreen(null);
            } else {
               message = "";
               cursor = 0;
               inputOffset = 0;
               historyPosition = 0;
            }
            break;

         case Keyboard.KEY_LEFT:
            // Moves the cursor left
            cursor--;
            break;

         case Keyboard.KEY_RIGHT:
            // Moves the cursor right
            cursor++;
            break;

         case Keyboard.KEY_DOWN:
            // Moves the history position down
            message = getInputHistory(--historyPosition);
            cursor = message.length();
            break;

         case Keyboard.KEY_UP:
            // Moves the history position down
            message = getInputHistory(++historyPosition);
            cursor = message.length();
            break;

         case Keyboard.KEY_DELETE:
            // Delete
            if (message.length() > 0) {
               validateCursor();
               String start = message.substring(0,cursor);
               String end = message.substring(cursor,message.length());
               this.message = start + (end.length() > 0 ? end.substring(1) : end);
            }
            break;

         case Keyboard.KEY_TAB:
            List<String> users = getPlayerNames();
            if (users != null) {
               String str = message;
               if (tabPosition != 0) {
                  str = tabWord;
                  if(tabPosition < 0){
                	  tabPosition = 0;
                  }
               } else if (message.contains(" ")) {
                  // Gets the last word in input
                  str = message.substring(message.lastIndexOf(" ") + 1,message.length());
               }
               boolean matched = false;
               List<String> matches = new ArrayList<String>();
               int numMatches = 0;
               for (int i = 0; i < users.size(); i++) {
                  String user = users.get(i);// % users.size());
                  // Tests if a username starts with the last input word (str)
                  if (user.toLowerCase().startsWith(str.toLowerCase())) {
                     matched = true;
                     matches.add(user);
                     tabWord = str;
                     numMatches++;
                  }
               }
               
               if(matched){
	               if (message.contains(" ")) {
	            	   message = message.substring(0, message.lastIndexOf(" ") + 1) + matches.get(tabPosition);
	               } else {
	            	   message = matches.get(tabPosition);
	               }
	               // Sets the cursor to the end of the input
	               cursor = message.length();
	               tabPosition++;
	               if(tabPosition >= numMatches){
	            	   tabPosition=-1;
	               }
               }
            }
            break;
            
         case Keyboard.KEY_BACK:
            // Backspace
            if (message.length() > 0) {
               validateCursor();
               String start = message.substring(0,cursor);
               String end = message.substring(cursor,message.length());
               this.message = start.substring(0,(start.length() - 1 > -1 ? start.length() - 1 : 0)) + end;
               cursor--;
               inputOffset--;
            }
            break;

         default:
            // Verifies that the character is in the character set before adding 
            if(ALLOWED_CHARACTERS.indexOf(key) >= 0 && this.message.length() < INPUT_MAX) { 
               validateCursor();
               String start = message.substring(0,cursor);
               String end = message.substring(cursor,message.length());
               this.message = start + key + end;
               cursor++;
            }
      }
   }
   
   /**
    * Gets all the usernames on the current server you're on
    * 
    * @return A list in alphabetical order of players logged onto the server
    */
   public List<String> getPlayerNames() {
	   List<String> names;
	   if(mc.theWorld.isRemote && mc.thePlayer instanceof EntityClientPlayerMP){
		   names = new ArrayList<String>();
		   NetClientHandler netclienthandler = ((EntityClientPlayerMP)mc.thePlayer).sendQueue;
		   List<GuiSavingLevelString> tempList = netclienthandler.playerNames;
		   for(GuiSavingLevelString string: tempList){
			   names.add(string.name);
		   }
	   }else{
		   names = null;
	   }

	   return names;
   }

   /**
    * Cleans a dirty string of any invalid characters then returns the clean 
    * string to the user. Verifies that the string doesn't go beyond the maximum
    * length as well
    * 
    * @param dirty - The string to clean
    * @return A nice clean string
    */
   public static String cleanString(String dirty) {
      String clean = "";
      if (dirty == null) {
         return "";
      }
      char letters[] = dirty.toCharArray();
      for (char letter : letters) {
         if (ALLOWED_CHARACTERS.indexOf(letter) >= 0) {
            clean += letter;
         }
      }
      if (clean.length() >= INPUT_MAX) {
         clean = clean.substring(0,INPUT_MAX - 1);
      }
      return clean;
   }

   /**
    * Gets the String on the system clip board, if it exists. Otherwise null is 
    * returned
    * 
    * @return Returns the String on the clip board
    */
   public static String getClipboardString() {
      try {
         Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
         if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return cleanString((String)t.getTransferData(DataFlavor.stringFlavor));
         }
      } catch (Exception e) {}
      return null;
   }

   /**
    * Sets a String onto the system clip board.
    * 
    * @param str - The string to copy onto the clip board
    */
   public static void setClipboardString(String str) {
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
      } catch (Exception e) {}
   }

   /**
    * Command history implementation, sets the historyPosition pointer to 
    * position based on its validity. The validity is verified by the method
    * and the pointer is kept between the bounds of the history
    * 
    * @param position - The position to set the pointer to
    * @return The input at this position in history
    */
   private String getInputHistory(int position) {
      if (INPUT_HISTORY.size() == 0) {
         return "";
      }
      if (position <= 0) {
         position = 0;
         return "";
      }
      if (position > INPUT_HISTORY.size()) {
         position = INPUT_HISTORY.size();
      }
      historyPosition = position;
      return INPUT_HISTORY.elementAt(INPUT_HISTORY.size() - historyPosition);
   }

   /**
    * Validates that the cursor is in a valid position, if the cursor isn't
    * then the cursor is moved into the closest valid position.
    */
   private void validateCursor() {
      if (cursor > message.length()) {
         cursor = message.length();
      } else if (cursor < 0) {
         cursor = 0;
      }
   }

   /**
    * Called per frame to draw the new frame
    * 
    * @see net.minecraft.src.GuiScreen#drawScreen(int, int, float)
    */
   @Override
   public void drawScreen(int mousex, int mousey, float f)
   {  
      // Background
      int minx = SCREEN_PADDING_LEFT;
      int miny = SCREEN_PADDING_TOP;
      int maxx = width-SCREEN_PADDING_RIGHT;
      int maxy = height-SCREEN_PADDING_BOTTOM;
      drawRect(minx,miny,maxx,maxy, COLOR_BASE);

      // Text box
      int textbox_minx = minx+BORDERSIZE;
      int textbox_maxx = maxx-BORDERSIZE;
      int textbox_miny = maxy-CHARHEIGHT-BORDERSIZE;
      int textbox_maxy = maxy-BORDERSIZE;
      drawRect(textbox_minx,textbox_miny,textbox_maxx,textbox_maxy,COLOR_INPUT_BACKGROUND);

      // Past messages - dialog
      int message_miny = miny+BORDERSIZE;
      int message_maxy = textbox_miny - BORDERSIZE;
      if (textbox_minx != MESSAGE_MINX || MESSAGE_MAXX != maxx-(BORDERSIZE*2)-10) {
         MESSAGE_MINX = textbox_minx;
         MESSAGE_MAXX = maxx-(BORDERSIZE*2)-10;
         buildLines();
      }

      if (LINES == null || rebuildLines) {
         buildLines();
      }

      drawRect(MESSAGE_MINX,message_miny,MESSAGE_MAXX,message_maxy,COLOR_OUTPUT_BACKGROUND);

      // Past messages - text
      int max = (message_maxy - message_miny) / CHARHEIGHT;
      if (slider != 0) {
         slider = LINES.size() - slider > max ? (LINES.size() - slider < LINES.size() ? slider : 0) : LINES.size() - max;
      }

      if (slider < 0) slider = 0;

      int oversize = 0;
      for (int i = 0; i + oversize < LINES.size() && i + oversize < max; i++) {
         int element = LINES.size() - 1 - i - slider;
         if (LINES.size() <= element) continue;
         fontRenderer.drawString(LINES.elementAt(element),MESSAGE_MINX + BORDERSIZE,textbox_miny - CHARHEIGHT - BORDERSIZE - ((i + oversize) * 10),COLOR_TEXT_OUTPUT);
      }

      // Scroll - background
      int scroll_minx = MESSAGE_MAXX + BORDERSIZE;
      int scroll_maxx = textbox_maxx;
      int scroll_miny = message_miny;
      int scroll_maxy = textbox_miny - BORDERSIZE;
      drawRect(scroll_minx, scroll_miny,scroll_maxx,scroll_maxy,COLOR_SCROLL_BACKGROUND);

      // Scroll - button top
      drawRect(scroll_minx + 1, scroll_miny + 1,scroll_maxx - 1,scroll_miny + 9,COLOR_SCROLL_FOREGROUND);
      //drawRect(scroll_minx + 2, scroll_miny + 2,scroll_maxx - 2,scroll_miny + 3,COLOR_SCROLL_SHADOW);
      //drawRect(scroll_minx + 2, scroll_miny + 2,scroll_minx + 3,scroll_miny + 8,COLOR_SCROLL_SHADOW);
      TOP = new int[] {scroll_minx + 1, scroll_miny + 1,scroll_maxx - 1,scroll_miny + 9};
      drawString(this.mc.fontRenderer, "^", TOP[0] + 2, TOP[1] + 2, COLOR_SCROLL_ARROW);

      // Scroll - button bottom
      drawRect(scroll_minx + 1, scroll_maxy - 9,scroll_maxx - 1,scroll_maxy - 1,COLOR_SCROLL_FOREGROUND);
      BOTTOM = new int[] {scroll_minx + 1, scroll_maxy - 9,scroll_maxx - 1,scroll_maxy - 1};
      drawStringFlipped(this.mc.fontRenderer, "^", BOTTOM[0] + 1, BOTTOM[1] - 3, COLOR_SCROLL_ARROW, true);

      // Scroll - bar           
      int scrollable_minx = scroll_minx + 1;
      int scrollable_maxx = scroll_maxx - 1;
      int scrollable_miny = scroll_miny + 11;
      int scrollable_maxy = scroll_maxy - 10;
      sliderHeight = scrollable_maxy - scrollable_miny;
      double heightpercentage = (double)max / (double)LINES.size();
      double barheight = (sliderHeight) * heightpercentage;
      barheight = barheight < 5 ? 5 : barheight;
      double stepsize = (sliderHeight - barheight) / (double)(LINES.size() - max);
      double position = slider * stepsize;

      if (LINES.size() < max) {
         drawRect(scrollable_minx,scrollable_miny,scrollable_maxx,scrollable_maxy,COLOR_SCROLL_FOREGROUND);
         BAR = new int[] {scrollable_minx,scrollable_miny,scrollable_maxx,scrollable_maxy};
      } else {
         drawRect(scrollable_minx, (int)(scrollable_maxy-position-barheight),scrollable_maxx,(int)(scrollable_maxy-position),COLOR_SCROLL_FOREGROUND);
         BAR = new int[] {scrollable_minx, (int)(scrollable_maxy-position-barheight),scrollable_maxx,(int)(scrollable_maxy-position)};
      }

      // Input
      validateCursor();
      String start = message.substring(0,cursor);
      String end = message.substring(cursor,message.length());
      String input = INPUT_PREFIX + start + ((updateCounter / 8) % 2 != 0 ? "." : "!") + end;
      int upperbound = input.length();
      // Sets the text to render based off where the cursor is currently positioned
      if (fontRenderer.getStringWidth(input) >= (textbox_maxx - textbox_minx)) {
         if (inputOffset < 0) {
            inputOffset = 0;
         }
         while (cursor >= inputOffset) {
            if (fontRenderer.getStringWidth(input.substring(inputOffset)) >= (textbox_maxx - textbox_minx)) {
               inputOffset++;
            } else {
               break;
            }
         }
         while (cursor < inputOffset && inputOffset > 0) {
            inputOffset--;
            while (fontRenderer.getStringWidth(input.substring(inputOffset,upperbound)) >= (textbox_maxx - textbox_minx)) {
               upperbound--;
            }
         }
         if (upperbound > input.length()) upperbound = input.length();
         input = input.substring(inputOffset,upperbound);
      }
      fontRenderer.drawString(input,textbox_minx + BORDERSIZE,textbox_miny + 1,COLOR_INPUT_TEXT);

      // Titlebar
      drawRect(maxx / 2, 0, maxx, miny, COLOR_BASE);

      // Title
      fontRenderer.drawString(TITLE, (maxx / 2) + BORDERSIZE,BORDERSIZE,COLOR_TEXT_TITLE);

      // Exit button
      drawRect(maxx - BORDERSIZE - 10, BORDERSIZE, maxx - BORDERSIZE, miny, COLOR_EXIT_BUTTON);
      fontRenderer.drawString("X",maxx-BORDERSIZE-7,BORDERSIZE+2,COLOR_EXIT_BUTTON_TEXT);
      EXIT = new int[] {maxx - BORDERSIZE - 10, BORDERSIZE, maxx - BORDERSIZE, miny};

      super.drawScreen(mousex, mousey, f);
   }

   /**
    * Draws the specified String flipped upside down
    * 
    * @param fontrenderer
    * @param s
    * @param i
    * @param j
    * @param k
    */
   public void drawStringFlipped(FontRenderer fontrenderer, String s, int i, int j, int k, boolean flag) {
      GL11.glPushMatrix();
      GL11.glScalef(-1F, -1F, 1F);
      GL11.glTranslatef((-i * 2) - fontrenderer.getStringWidth(s), (-j * 2) - fontrenderer.FONT_HEIGHT, 0.0F);
      if(flag)
    	  fontrenderer.drawString(s, i - 1, j - 1, (k & 0xfcfcfc) >> 2 | k & 0xff000000); //Took the last argument from FrontRenderer.renderString() because it's private and I want the shadow on the correct side when flipped 
      fontrenderer.drawString(s, i, j, k);
      GL11.glPopMatrix();
   }

   /**
    * Called on mouse clicked and processes the button clicks and actions
    * 
    * @see net.minecraft.src.GuiScreen#mouseClicked(int, int, int)
    */
   @Override
   protected void mouseClicked(int mousex, int mousey, int button) {
      if(button == 0) {
         if (mousex >= EXIT[0] && mousex <= EXIT[2] && mousey >= EXIT[1] && mousey <= EXIT[3]) {
            mc.displayGuiScreen(null);
            return;
         }
         if (mousex >= TOP[0] && mousex <= TOP[2] && mousey >= TOP[1] && mousey <= TOP[3]) slider++;
         if (mousex >= BOTTOM[0] && mousex <= BOTTOM[2] && mousey >= BOTTOM[1] && mousey <= BOTTOM[3]) slider--;
         if (mousex >= BAR[0] && mousex <= BAR[2] && mousey >= BAR[1] && mousey <= BAR[3]) {
            isSliding = true;
            lastSliding = mousey;
            initialSliding = slider;
         }

         if(this.mc.ingameGUI.field_933_a != null) {
            if(this.message.length() > 0 && !this.message.endsWith(" ")) {
               this.message = this.message + " ";
            }

            this.message = this.message + this.mc.ingameGUI.field_933_a;
            byte var4 = 100;
            if(this.message.length() > var4) {
               this.message = this.message.substring(0, var4);
            }
         } else {
            super.mouseClicked(mousex, mousey, button);
         }
      }
   }

   /**
    * Method is called on mouse movement and used to determine slider movement
    * 
    * @see net.minecraft.src.GuiScreen#mouseMovedOrUp(int, int, int)
    */
   @Override
   protected void mouseMovedOrUp(int mousex, int mousey, int button) {
      int wheel = Mouse.getDWheel();
      if (wheel != 0) {
         slider += wheel / 120;
      }

      if (isSliding) {
         if (Mouse.isButtonDown(0)) {
            int diff = lastSliding - mousey;
            if (diff != 0) {
               slider = initialSliding + (int)((diff / (double)sliderHeight) * (LINES.size()));
            }
         } else {
            isSliding = false;
            lastSliding = 0;
            initialSliding = 0;
         }
      }
   }

   /**
    * Returns true if the GUI is open
    * 
    * @return Returns whether the GUI is open or not
    */
   public boolean isGuiOpen() {
      return isGuiOpen;
   }

   /**
    * Adds a console listener to the console. When events are triggered they 
    * are then sent to all the listeners in the order which they are registered
    * in
    * 
    * @param cl - The listener to add
    */
   public void addConsoleListener(ConsoleListener cl) {
      if (!LISTENERS.contains(cl)) {
         LISTENERS.add(cl);
      }
   }

   /**
    * Adds an input message to the console
    * 
    * @param message - The input message
    */
   public void addInputMessage(String message) {
      if (PRINT_INPUT) {
         MESSAGES.add(INPUT_PREFIX + message);
         addLine(INPUT_PREFIX + message);
      }
      INPUT_HISTORY.add(message);
      if ((LOGGING & LOGGING_INPUT) > 0) {
         log.add(INPUT_PREFIX + message);
      }
      for (ConsoleListener cl : LISTENERS) {
         cl.processInput(message);
      }
   }

   /**
    * Adds an output message to the console
    * 
    * @param message - The output message
    */
   public void addOutputMessage(String message) {
      if (PRINT_OUTPUT) {
         MESSAGES.add(message);
         addLine(message);
      }
      if ((LOGGING & LOGGING_OUTPUT) > 0) {
         log.add(message);
      }
      for (ConsoleListener cl : LISTENERS) {
         cl.processOutput(message);
      }
   }

   private void pullGuiList() {
      if (getInGameGuiList() && IN_GAME_GUI.size() > 0 && !((ChatLine)IN_GAME_GUI.get(0)).equals(lastOutput)) {
         int pointer = 0;
         while (pointer < IN_GAME_GUI.size()) {
            if (((ChatLine)IN_GAME_GUI.get(pointer)).equals(lastOutput)) {
               break;
            }
            pointer++;
         }
         --pointer;
         for (int i = pointer; i > -1; i--) {
            addOutputMessage(((ChatLine)IN_GAME_GUI.get(i)).message);
         }
         lastOutput = (ChatLine)IN_GAME_GUI.get(0);
      }
   }

   /**
    * Runs a thread which automatically pulls the chat line into the console 
    * on a configurable interval, by default 20ms. It uses Object.equals 
    * against the ChatLine object to determine the previous message which was
    * copied across. 
    * 
    * This method also clears the history and output lists once they reach 
    * capacity
    * 
    * This method also handles key bindings
    * 
    * @see java.lang.Runnable#run()
    */
   @Override
   public void run() {
      while (true) {
         try {
            if (logName == null) {
               logName = (new SimpleDateFormat(DATE_FORMAT_FILENAME)).format(new Date());
            }
            if (lastWrite + LOG_WRITE_INTERVAL < System.currentTimeMillis()) {
               try {
                  FileOutputStream fos = new FileOutputStream(new File(LOG_DIR,logName),true);
                  while (log.size() > 0) {
                     String line = sdf.format(new Date()) + log.elementAt(0) + LINE_BREAK;
                     fos.write(line.getBytes());
                     log.remove(0);
                  }
                  lastWrite = System.currentTimeMillis();
               } catch (FileNotFoundException e) {}
            }
            // Pull across GuiList output
            pullGuiList();

            // Empties message list when it hits maximum size
            while (MESSAGES.size() > OUTPUT_MAX) {
               MESSAGES.remove(0);
               rebuildLines = true;
            }
            // Empties input history list when it hits maximum size
            while (INPUT_HISTORY.size() > INPUT_HISTORY_MAX) {
               INPUT_HISTORY.remove(0);
            }

            // Verify key down list items are still down - repeat event workaround
            for (int i = 0; i < keyDown.size(); i++) {
               if (!Keyboard.isKeyDown(keyDown.get(i))) {
                  keyDown.remove(keyDown.get(i));
               }
            }

            // Key bindings
            // String <code,code,code,...> : value > HashMap<String(codes),String(value)>
            Iterator<String> i = keyBindings.keySet().iterator();
            while (i.hasNext()) {
               String k = i.next();
               if (k == null) {
                  continue;
               }
               String keys[] = k.split(",");
               try {
                  boolean execute = true;
                  int keydown = 0;
                  for (String key : keys) {
                     int keyvalue = Integer.parseInt(key);
                     if (!Keyboard.isKeyDown(keyvalue)) {
                        execute = false;
                        break;
                     }
                     if (keyDown.contains(keyvalue)) {
                        keydown++;
                     }
                     keyDown.add(keyvalue);
                  }
                  if (execute && keydown < keys.length) {
                     if (BACKGROUND_BINDING_EVENTS || mc.currentScreen == null) {
                        // Adds binding message to input: addInputMessage(keyBindings.get(k));
                        mc.thePlayer.sendChatMessage(keyBindings.get(k));
                     }
                  }
               } catch (Exception e) { // If the number can't parse it is invalid anyway
               }
            }

            Thread.sleep(POLL_DELAY);
         } catch (Exception e) { 
            e.printStackTrace();
         }
      }
   }

   /**
    * Reads the settings from the specified file and sets them to their 
    * applicable variables
    * 
    * @param base - The class to set the settings in
    * @param settings - The settings file to load the values from
    */
   public static void readSettings(Class<?> base, File settings) {
      Properties p = new Properties();
      try {
         p.load(new FileInputStream(settings));
         Field[] declaredFields = base.getDeclaredFields();
         for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
               try {
                  if (!field.isAccessible()) {
                     field.setAccessible(true);
                  }

                  if (field.getType().equals(String.class)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                    	 field.set(null, property);
                     }
                  } else if (field.getType().equals(Integer.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Integer.decode(property).intValue());
                     }
                  } else if (field.getType().equals(Double.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Double.parseDouble(property));
                     }
                  } else if (field.getType().equals(Boolean.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Boolean.parseBoolean(property));
                     }
                  } else if (field.getType().equals(Long.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Long.decode(property).longValue());
                     }
                  } else if (field.getType().equals(Byte.TYPE)) { // new
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Byte.parseByte(property));
                     }
                  } else if (field.getType().equals(Float.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Float.parseFloat(property));
                     }
                  } else if (field.getType().equals(Short.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, Short.decode(property));
                     }
                  } else if (field.getType().equals(Character.TYPE)) {
                     String property = (String)p.get(field.getName());
                     if (property != null) {
                        field.set(null, property.charAt(0));
                     }
                  }
               } catch (Exception e) {}
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Writes the variables from the specified class into the specified file
    * in a .properties format
    * 
    * @param base - The class to get the settings from
    * @param settings - The settings file to save the values to
    */
   public static void writeSettings(Class<?> base, File settings) {
      Properties p = new Properties();
      try {
         try {
            p.load(new FileInputStream(settings));
         } catch (Exception e) {}
         Field[] declaredFields = base.getDeclaredFields();
         for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
               try {
                  if (!field.isAccessible()) {
                     field.setAccessible(true);
                  }
                  if (field.getType().equals(Integer.TYPE)) {
                     p.setProperty(field.getName(), field.getInt(null) + "");
                  } else if (field.getType().equals(Double.TYPE)) {
                     p.setProperty(field.getName(), field.getDouble(null) + "");
                  } else if (field.getType().equals(Boolean.TYPE)) {
                     p.setProperty(field.getName(), field.getBoolean(null) + "");
                  } else if (field.getType().equals(Long.TYPE)) {
                     p.setProperty(field.getName(), field.getLong(null) + "");
                  } else if (field.getType().equals(String.class)) {
                		 p.setProperty(field.getName(), (String)field.get(null));
                  } else if (field.getType().equals(Byte.TYPE)) {
                     p.setProperty(field.getName(), field.getByte(null) + "");
                  } else if (field.getType().equals(Short.TYPE)) {
                     p.setProperty(field.getName(), field.getShort(null) + "");
                  } else if (field.getType().equals(Float.TYPE)) {
                     p.setProperty(field.getName(), field.getFloat(null) + "");
                  } else if (field.getType().equals(Character.TYPE)) {
                     p.setProperty(field.getName(), field.getChar(null) + "");
                  }
               } catch (Exception e) {}
            }
         }
         p.store(new FileOutputStream(settings), "");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}