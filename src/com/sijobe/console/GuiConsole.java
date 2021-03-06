package com.sijobe.console;

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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.src.ChatAllowedCharacters;
import net.minecraft.src.ChatLine;
import net.minecraft.src.EntityClientPlayerMP;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.GuiIngame;
import net.minecraft.src.GuiPlayerInfo;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetClientHandler;
import net.minecraft.src.mod_Console;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.vayner.console.ConsoleChatCommands;

/**
 * @formatter:off
 *                TODO: P1 - Only save logs for the current world
 *                TODO: P1 - Output filtering - allow blocking of certain text/people
 *                DONE: p2 - Text selection in the chat-history field (copy text)
 *                TODO: P2 - Spinner (tab auto complete)
 *                TODO: P3 - Drop down menus
 *                TODO: P2 - Improve look and feel
 *                TODO: P2 - Custom text color support. Holding CTRL then type a number will set the text to that color [0-f] - (0-15)
 *                TODO: P1 - Add ability to disable settings loader (in code) and ability to reset the settings ingame
 *                TODO: P3 - Dynamic settings screen, configure any setting in an easy to use GUI
 *
 * @author simo_415
 *
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU Lesser General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or
 *         (at your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful,
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *         GNU Lesser General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program. If not, see <http://www.gnu.org/licenses/>.
 * @formatter:on
 */
public class GuiConsole extends GuiScreen implements Runnable {
   /* @formatter:off */
   protected String message;                                         // The current user input
   protected String input;                                           // The current input line to draw (includes prefix)
   private int updateCounter;                                        // The tick count - used for cursor blink rate
   private int slider;                                               // Position of the scroll bar
   private int cursor;                                               // Position of the cursor
   private int inputOffset;                                          // Position in the message string where the input goes
   private int sliderHeight;                                         // Height of the scroll bar
   private boolean isHighlighting;                                   // Keeps track of the highlight mouse click
   private int[] initialHighlighting = new int[2];                   // Position of the mouse (at character) initially for highlighting
   private int[] lastHighlighting = new int[2];                      // Position of the mouse (at character) at end of highlighting
   private boolean isSliding;                                        // Keeps track of the slider mouse click
   private int lastSliding;                                          // Position of mouse at last frame for slider
   private int initialSliding;                                       // Position of mouse initially for slider
   private int historyPosition;                                      // Position of where in the history you are at
   private boolean isGuiOpen;                                        // When the console is open this is true
   private ChatLine lastOutput;                                      // The last message to go into the Minecraft chatline
   private boolean rebuildLines;                                     // Keeps track of whether the lines list needs to be rebuilt
   private volatile Vector<String> log;                              // The log messages
   private SimpleDateFormat sdf;                                     // The date format for logs

   private int tabPosition;                                          // Where you have tabbed to through user list
   private int tabWordPos;                                           //start place of tabbed word
   private String tabWord;                                           // The current tabbed word
   private String tabMessage;                                        // The current tabbed message

   private volatile HashMap<String,String> keyBindings;              // All the current key bindings
   private volatile List<Integer> keyDown;                           // List of all the keys currently held down
   private static boolean BACKGROUND_BINDING_EVENTS = false;         // Allows the bindings to run ingame with different GUIs open

   private String logName;                                           // The name of the log file to write
   private long lastWrite;                                           // The time of the last log write

   private static final String ALLOWED_CHARACTERS;                   // A list of permitted characters

   @Deprecated
   private static int MESSAGE_MAXX;                                  // Maximum size of the message GUI
   @Deprecated
   private static int MESSAGE_MINX;                                  // Minimum size of the message GUI

   public static Vector<String> INPUT_HISTORY;                       // All the input which went into the console
   private static Vector<String> LINES;                              // All of the lines to output
   private static Vector<String> MESSAGES;                           // All of the input/output
   private static Vector<ConsoleListener> LISTENERS;                 // All of the console listeners which were registered

   private static volatile List IN_GAME_GUI;                         // The ingamegui message list
   private static List IN_GAME_GUI_TEMP;                             // Used when the console is open to not display messages on INGAMEGUI

   private static int[] TOP;                                         // Poor implementation to keep track of drawn scrollbar top button
   private static int[] BOTTOM;                                      // Poor implementation to keep track of drawn scrollbar bottom button
   private static int[] BAR;                                         // Poor implementation to keep track of drawn scrollbar
   private static int[] EXIT;                                        // Poor implementation to keep track of drawn exit button
   private static int[] TEXT_BOX;												// Poor implementation to keep track of drawn text box
   private static int[] HISTORY;                                     // Poor implementation to keep track of drawn history field

   private static int CHARHEIGHT = 10;                               // Character height - used to quickly determine number of lines per view
   private static double CHARWIDTH = 6;                              // Maximum character width - used to quickly determine line length

   private static boolean CLOSE_ON_SUBMIT = false;                   // Closes the GUI after the input has been submit
   private static boolean SCROLL_TO_BOTTOM_ON_SUBMIT = true;         // Moves the scroll bar to the bottom when input is sumbitted
   private static boolean CLOSE_WITH_OPEN_KEY = true;                // Closes the GUI if the open key pressed again

   private static int INPUT_MAX = 150;                               // Maximum input size on the console
   private static int INPUT_SERVER_MAX = 100;                        // Maximum server message size - splits the input to this length if it is longer
   private static int INPUT_HISTORY_MAX = 50;                        // Maximum size of stored input history
   private static String INPUT_PREFIX = "> ";                        // Prefix for all input messages

   private static boolean PRINT_INPUT = true;                        // Prints the input
   private static boolean PRINT_OUTPUT = true;                       // Prints the output

   private static boolean HISTORY_BETWEEN_WORLDS = true;             // Not used

   public static final int LOGGING_TRACE = 8;                        // Logging level - Trace
   public static final int LOGGING_DEBUG = 4;                        // Logging level - Debug
   public static final int LOGGING_INPUT = 2;                        // Logging level - Input
   public static final int LOGGING_OUTPUT = 1;                       // Logging level - Output
   private static int LOGGING = LOGGING_INPUT + LOGGING_OUTPUT;      // What is currently being logged
   private static long LOG_WRITE_INTERVAL = 1000L;                   // How often (in ms) the logs are written to file
   // The log line separator
   private static String LINE_BREAK = System.getProperty("line.separator");

   private static String DATE_FORMAT_LOG = "yyyy-MM-dd hh:mm:ss: ";  // The date format according to SimpleDateFormat
   // The date format filename (uses SimpleDateFormat)
   private static String DATE_FORMAT_FILENAME = "yyyyMMdd_hhmmss'.log'";

   private static int OUTPUT_MAX = 200;                              // Maximum number of lines in the output

   private static long POLL_DELAY = 20L;                             // The amount of time (in ms) to run the thread at

   private static int LINES_PER_SCROLL = 1;                          // The number of lines to scroll in one scroll wheel click

   private static int BORDERSIZE = 2;                                // Size of the border
   private static int SCREEN_PADDING_LEFT = 5;                       // Size of the screen padding - left
   private static int SCREEN_PADDING_TOP = 12;                       // Size of the screen padding - top
   private static int SCREEN_PADDING_RIGHT = 5;                      // Size of the screen padding - right
   private static int SCREEN_PADDING_BOTTOM = 40;                    // Size of the screen padding - bottom

   private static int COLOR_BASE = 0x90000000;                       // Base colour to use for console
   private static int COLOR_SCROLL_BACKGROUND = 0xBB999999;          // Scroll background colour
   private static int COLOR_SCROLL_FOREGROUND = 0xBB404040;          // Scroll foreground colour
   private static int COLOR_INPUT_TEXT = 0xE0E0E0;                   // Colour of the input text
   private static int COLOR_TEXT_OUTPUT = 0xE0E0E0;                  // Colour of the text output
   private static int COLOR_TEXT_TITLE = 0xE0E0E0;                   // Colour of the text title
   private static int COLOR_TEXT_HIGHLIGHT = 0xFF2090DD;             // Colour of the text highlighting
   private static int COLOR_SCROLL_ARROW = 0xFFFFFF;                 // Colour of the scroll arrow
   private static int COLOR_EXIT_BUTTON_TEXT = 0xFFFFFF;             // Colour of the exit button label
   private static int COLOR_EXIT_BUTTON = 0xBB999999;                // Colour of the exit button
   private static int COLOR_OUTPUT_BACKGROUND = 0xBB999999;          // Colour of the output background
   private static int COLOR_INPUT_BACKGROUND = 0xBB999999;           // Colour of the input background

   public static final String VERSION = "1.2.1b";                    // Version of the mod
   private static String TITLE = "Console";                          // Title of the console

   private static final String MOD_PATH = "mods/console/";           // Relative location of the mod directory
   private static String LOG_PATH = "mods/console/logs";             // Relative location of the console logs
   // Mod directory
   public static File MOD_DIR = new File(Minecraft.getMinecraftDir(), MOD_PATH);
   private static File LOG_DIR;                                      // Log directory

   private static GuiConsole INSTANCE;                               // Instance of the class for singleton pattern

   private static boolean EMACS_KEYS = false;                        // Use emacs keybindings
   private static int AUTOCOMPLETE_KEY = Keyboard.KEY_TAB;           // Autocomlete keybinding

   /* @formatter:on */

   /**
    * Initialises all of the instance variables
    */
   static {
      if ((new File(MOD_DIR, "gui.properties")).exists()) {
         readSettings(GuiConsole.class, new File(MOD_DIR, "gui.properties"));
      }
      writeSettings(GuiConsole.class, new File(MOD_DIR, "gui.properties"));
      LOG_DIR = new File(Minecraft.getMinecraftDir(), LOG_PATH);
      ALLOWED_CHARACTERS = ChatAllowedCharacters.allowedCharacters;
      MESSAGES = new Vector<String>();
      MESSAGES.add("\2476Minecraft Console version \2474" + VERSION + "\2476 written by \2472simo_415\2476, updated by \2473tellefma");
      MESSAGES.add("");
      INPUT_HISTORY = new Vector<String>();
      LISTENERS = new Vector<ConsoleListener>();
      TOP = new int[4];
      BOTTOM = new int[4];
      BAR = new int[4];
      EXIT = new int[4];
      TEXT_BOX = new int[4];
      INSTANCE = new GuiConsole();
      if (!MOD_DIR.exists()) {
         try {
            MOD_DIR.mkdirs();
         } catch (Exception e) {
         }
      }
      if (!LOG_DIR.exists()) {
         try {
            LOG_DIR.mkdirs();
         } catch (Exception e) {
         }
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
      addConsoleListener(new ConsoleSettingCommands());
   }

   /**
    * Loads the core set of classes which handle player input/output.
    */
   private void loadCoreCommands() {
      ;
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
   public HashMap<String, String> generateKeyBindings() {
      Properties p = new Properties();
      HashMap<String, String> bindings = new HashMap<String, String>();
      try {
         p.load(new FileInputStream(new File(MOD_DIR, "bindings.properties")));
         Iterator i = p.keySet().iterator();
         while (i.hasNext()) {
            String o = (String) i.next();
            bindings.put(o, (String) p.get(o));
         }
      } catch (Exception e) {
      }
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
                  IN_GAME_GUI = (List<?>) o;
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
      Vector<String> temp = (Vector<String>) MESSAGES.clone(); //There were problems with the run method modifying MESSAGES while this loop works. See issue #17
      for (String message : temp) {
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
      int chars = (int) ((MESSAGE_MAXX - MESSAGE_MINX) / CHARWIDTH);
      String parts[] = message.split("(?<=\\G.{0," + (chars) + "}? )");
      String temp = "";
      int linesAdded = 0;
      for (int i = 0; i < parts.length; i++) {
         if (parts[i].length() > chars) {
            String parts2[] = parts[i].split("(?<=\\G.{" + temp.length() + "," + (chars) + "}+)");
            LINES.add(temp + parts2[0]);
            linesAdded++;
            for (int j = 1; j < parts2.length; j++) {
               if (parts2[j].length() < chars) {
                  temp = parts2[j];
               } else {
                  LINES.add(parts2[j]);
                  linesAdded++;
               }
            }
         } else {
            if (temp.length() + parts[i].length() <= chars) {
               temp += parts[i];
            } else {
               LINES.add(temp);
               linesAdded++;
               temp = parts[i];
            }
         }

         if (i + 1 >= parts.length) {
            LINES.add(temp);
            linesAdded++;
         }
      }

      /* if(initialHighlighting[0] > -1 && lastHighlighting[0] > -1){
          initialHighlighting[0] -= linesAdded;
          lastHighlighting[0] -= linesAdded;
       }*/
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
      isHighlighting = false;
      clearHighlighting();
      cursor = 0;
      message = "";
      updateCounter = 0;
      historyPosition = 0;
      isGuiOpen = true;
      rebuildLines = true;
      pullGuiList();
      try {
         if (getInGameGuiList()) {
            for (Object message : IN_GAME_GUI) {
               IN_GAME_GUI_TEMP.add(0, message);
            }
            IN_GAME_GUI.clear();
         }
      } catch (Exception e) {
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
      try {
         if (!isGuiOpen) {
            for (Object message : IN_GAME_GUI_TEMP) {
               IN_GAME_GUI.add(0, message);
            }
            IN_GAME_GUI_TEMP.clear();
         }
      } catch (Exception e) {
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
      try {
         pullGuiList();
         if (isGuiOpen) {
            for (int i = IN_GAME_GUI.size() - 1; i > -1; i--) {
               IN_GAME_GUI_TEMP.add(IN_GAME_GUI.get(i));
            }
            IN_GAME_GUI.clear();
         }
      } catch (Exception e) {
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
            if (initialHighlighting[0] != -1 && lastHighlighting[0] != -1) {
               String clipboard = "";
               int firstInLINES = initialHighlighting[0] <= lastHighlighting[0] ? initialHighlighting[0] : lastHighlighting[0];

               if (initialHighlighting[0] == lastHighlighting[0]) {
                  int firsti, lasti;
                  if (initialHighlighting[1] < lastHighlighting[1]) {
                     firsti = initialHighlighting[1];
                     lasti = lastHighlighting[1];
                  } else {
                     firsti = lastHighlighting[1];
                     lasti = initialHighlighting[1];
                  }
                  clipboard = LINES.get(firstInLINES).substring(firsti, lasti);
               } else {
                  for (int i = 0; i < Math.abs(initialHighlighting[0] - lastHighlighting[0]); i++) {
                     String temp = LINES.get(firstInLINES + i);
                     if (firstInLINES + i == initialHighlighting[0]) {
                        if (initialHighlighting[0] < lastHighlighting[0]) {
                           temp = temp.substring(initialHighlighting[1]);
                        } else {
                           temp = temp.substring(0, initialHighlighting[1]);
                        }
                     } else if (firstInLINES + i == lastHighlighting[0]) {
                        if (initialHighlighting[0] > lastHighlighting[0]) {
                           temp = temp.substring(lastHighlighting[1]);
                        } else {
                           temp = temp.substring(0, lastHighlighting[1]);
                        }
                     }

                     clipboard += temp + " ";
                  }
               }

               setClipboardString(clipboard.trim());
            } else {
               if (lastHighlighting[1] != initialHighlighting[1] && initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                  if (initialHighlighting[1] < lastHighlighting[1]) {
                     setClipboardString(message.substring(initialHighlighting[1], lastHighlighting[1]));
                  } else {
                     setClipboardString(message.substring(lastHighlighting[1], initialHighlighting[1]));
                  }
               }
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
            paste();
         } else if (Keyboard.isKeyDown(Keyboard.KEY_X)) {
            if (initialHighlighting[1] != lastHighlighting[1] && initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
               String start, end;
               if (initialHighlighting[1] < lastHighlighting[1]) {
                  setClipboardString(message.substring(initialHighlighting[1], lastHighlighting[1]));
                  start = message.substring(0, initialHighlighting[1]);
                  end = message.substring(lastHighlighting[1]);
               } else {
                  setClipboardString(message.substring(lastHighlighting[1], initialHighlighting[1]));
                  start = message.substring(0, lastHighlighting[1]);
                  end = message.substring(initialHighlighting[1]);
               }

               message = start + end;
               initialHighlighting[0] = lastHighlighting[0] = -1;
               initialHighlighting[1] = lastHighlighting[1] = 0;
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            if (!EMACS_KEYS) {
               if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                  initialHighlighting[1] = 0;
                  lastHighlighting[1] = message.length();
               } else {
                  initialHighlighting[0] = 0;
                  initialHighlighting[1] = 0;
                  lastHighlighting[0] = LINES.size() - 1;
                  lastHighlighting[1] = LINES.get(LINES.size() - 1).length();
               }
            } else {
               // go to beginning of line
               cursor = 0;
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
            if (EMACS_KEYS) {
               // go to end of line
               cursor = message.length();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
            if (EMACS_KEYS && initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
               // Cut to end of line
               setClipboardString(message.substring(cursor, message.length()));

               message = message.substring(0, cursor);
               clearHighlighting();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_Y)) {
            if (EMACS_KEYS) {
               paste();
            }
         } else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            if (EMACS_KEYS) {
               delete();
            }
         }
         return;
      }

      if (id != AUTOCOMPLETE_KEY) {
         tabPosition = 0;
         tabWord = "";
      }
      else
      {
         clearHighlighting();
         if (message.startsWith("@get ") || message.startsWith("@list ") || message.startsWith("@set ")) {
            String[] str = message.split(" ");

            String match = "";

            if (str.length > 1) {
               if (tabPosition == 0) {
                  match = str[1];
               } else {
                  match = tabWord;
               }
            }
            if (tabPosition < 0) {
               tabPosition = 0;
            }

            if (cursor >= str[0].length() + 1 && cursor <= str[0].length() + 1 + match.length() || tabPosition > 0) {
               ArrayList<String> tempList = new ArrayList<String>(Arrays.asList(ConsoleSettingCommands.list("").split("\n")));
               ArrayList<String> list = new ArrayList<String>();
               for (int i = 0; i < tempList.size(); i++) {
                  if (tempList.get(i).startsWith(match.toUpperCase())) {
                     list.add(tempList.get(i)); //Can't delete from a list in a loop; workaround
                  }
               }

               if (list.size() > 0) {
                  tabWord = match;

                  if (tabPosition == 0) {
                     message = message.substring(0, str[0].length() + 1) + list.get(tabPosition) + message.substring(str[0].length() + 1 + match.length(), message.length());
                  } else if (tabPosition > 0) {
                     message = message.substring(0, str[0].length() + 1) + list.get(tabPosition) + message.substring(str[0].length() + 1 + list.get(tabPosition - 1).length(), message.length());
                  }
                  cursor = str[0].length() + 1 + list.get(tabPosition).length();

                  tabPosition++;
                  if (tabPosition >= list.size()) {
                     tabPosition = -1;
                  }
               }
            }
         } else {
            List<String> autoWords = getAutoPossibility();
            int word = 0;
            int pos = 0;
            String str = "";
            if (autoWords != null) {
               str = message;
               if (tabPosition != 0) {
                  str = tabWord;
                  if (tabPosition < 0) {
                     tabPosition = 0;
                  }
               }

               if (message.contains(" ")) {
                  // Gets the word to tab-complete
                  String[] words = message.split(" ");
                  if (words.length > 0) {
                     for (int i = 0; i < words.length; i++) {
                        int spacesBefore = 0; //I had to do this instead of indexOf so we can still tab complete if it isn't the first instance of the word in the string
                        if (pos != 0) {
                           String temp = ("." + message.substring(pos)).trim(); //Added dot to keep beginning spaces
                           temp = temp.substring(1); //get rid of dot
                           spacesBefore = temp.length() - temp.trim().length(); //length with spaces - length without spaces.
                        }
                        pos += words[i].length() + spacesBefore;
                        if (cursor <= pos) {
                           word = i;
                           pos -= words[i].length(); //Pos now represents the beginning of the word to replace
                           break;
                        }
                     }

                     str = words[word];
                  } else {
                     str = "";
                  }
               }

               boolean matched = false;
               ArrayList<String> matches = new ArrayList<String>();
               int numMatches = 0;
               for (int i = 0; i < autoWords.size(); i++) {
                  String currentWord = autoWords.get(i);
                  // Tests if a autoword starts with the last input word (str)
                  if (currentWord.toLowerCase().startsWith(str.toLowerCase())) {
                     matched = true;
                     matches.add(currentWord);
                     tabWord = str;
                     numMatches++;
                  }
               }

               if (matched) {
                  if (tabPosition >= numMatches) { //If a player leaves in-between tabs tabPosition might be out of bounds
                     tabPosition = 0;
                  }

                  if (message.contains(" ")) {
                     String replace = str;
                     message = message.substring(0, pos) + matches.get(tabPosition) + message.substring(pos + replace.length(), message.length());
                     cursor = pos + matches.get(tabPosition).length();
                  } else {
                     message = matches.get(tabPosition);
                     cursor = message.length();
                  }

                  tabPosition++;
                  if (tabPosition >= numMatches) {
                     tabPosition = -1;
                  }
               }
            }
         }
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
            }

            if (CLOSE_ON_SUBMIT) {
               mc.displayGuiScreen(null);
            }

            if (SCROLL_TO_BOTTOM_ON_SUBMIT) {
               slider = 0;
            }

            message = "";
            cursor = 0;
            inputOffset = 0;
            historyPosition = 0;
            clearHighlighting();
            break;

         case Keyboard.KEY_LEFT:
            // Moves the cursor left
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
               if (initialHighlighting[1] == lastHighlighting[1]) {
                  if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                     initialHighlighting[1] = cursor;
                     lastHighlighting[1] = cursor;
                  }
               }

               lastHighlighting[1]--;
               if (lastHighlighting[1] < 0) {
                  lastHighlighting[1] = 0;
                  if (lastHighlighting[0] > 0) {
                     lastHighlighting[0]--;
                  }
               }
               validateHighlighting();
            } else {
               clearHighlighting();
            }
            cursor--;
            break;

         case Keyboard.KEY_RIGHT:
            // Moves the cursor right
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
               if (initialHighlighting[1] == lastHighlighting[1]) {
                  if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
                     initialHighlighting[1] = cursor;
                     lastHighlighting[1] = cursor;
                  }
               }

               lastHighlighting[1]++;
               if (lastHighlighting[0] == -1 && initialHighlighting[0] == -1) {
                  if (lastHighlighting[1] > message.length()) {
                     lastHighlighting[1] = message.length();
                  }
               } else {
                  if (lastHighlighting[1] > LINES.get(lastHighlighting[0]).length()) {
                     lastHighlighting[1] = LINES.get(lastHighlighting[0]).length();
                     if (lastHighlighting[0] < LINES.size() - 1) {
                        lastHighlighting[0]++;
                     }
                  }
               }
               validateHighlighting();
            } else {
               clearHighlighting();
            }
            cursor++;
            break;

         case Keyboard.KEY_DOWN:
            // Moves the history position down
            message = getInputHistory(--historyPosition);
            cursor = message.length();
            clearHighlighting();
            break;

         case Keyboard.KEY_UP:
            // Moves the history position down
            message = getInputHistory(++historyPosition);
            cursor = message.length();
            clearHighlighting();
            break;

         case Keyboard.KEY_DELETE:
            delete();
            break;

         case Keyboard.KEY_TAB:
            //moved
            break;

         case Keyboard.KEY_BACK:
            // Backspace
            if (message.length() > 0) {
               if (initialHighlighting[1] == lastHighlighting[1] || initialHighlighting[0] != -1 || lastHighlighting[0] != -1) {
                  validateCursor();
                  String start = message.substring(0, cursor);
                  String end = message.substring(cursor, message.length());
                  this.message = start.substring(0, (start.length() - 1 > -1 ? start.length() - 1 : 0)) + end;
                  cursor--;
                  inputOffset--;
                  if (inputOffset < 0) {
                     inputOffset = 0;
                  }
               } else {
                  String start, end;
                  if (initialHighlighting[1] < lastHighlighting[1]) {
                     start = message.substring(0, initialHighlighting[1]);
                     end = message.substring(lastHighlighting[1]);
                  } else {
                     start = message.substring(0, lastHighlighting[1]);
                     end = message.substring(initialHighlighting[1]);
                  }
                  inputOffset -= Math.abs(lastHighlighting[1] - initialHighlighting[1]);
                  if (inputOffset < 0) {
                     inputOffset = 0;
                  }
                  message = start + end;
               }
               clearHighlighting();
            }
            break;
         case Keyboard.KEY_HOME:
            cursor = 0;
            clearHighlighting();
            break;
         case Keyboard.KEY_END:
            cursor = message.length();
            clearHighlighting();
            break;
         default:
            // Verifies that the character is in the character set before adding
            if (updateCounter != 0) {
               if (CLOSE_WITH_OPEN_KEY && id == mod_Console.openKey.keyCode) {
                  mc.displayGuiScreen(null);
                  break;
               }
               if (ALLOWED_CHARACTERS.indexOf(key) >= 0 && this.message.length() < INPUT_MAX && !(message.startsWith("/") && message.length() > INPUT_SERVER_MAX - 1)) {
                  if (initialHighlighting[1] == lastHighlighting[1] || initialHighlighting[0] != -1 || lastHighlighting[1] != 1) {
                     validateCursor();
                     clearHighlighting();
                     String start = message.substring(0, cursor);
                     String end = message.substring(cursor, message.length());
                     this.message = start + key + end;
                     cursor++;
                  } else {
                     String start, end;
                     if (initialHighlighting[1] < lastHighlighting[1]) {
                        start = message.substring(0, initialHighlighting[1]);
                        end = message.substring(lastHighlighting[1]);
                     } else {
                        start = message.substring(0, lastHighlighting[1]);
                        end = message.substring(initialHighlighting[1]);
                     }

                     message = start + key + end;
                     cursor = start.length() + 1;
                     clearHighlighting();
                  }
               }
            }
      }
   }

   /**
    * Returns true if the current game is being player on a server
    *
    * @return True is returned when the current game is being played on a
    *         Minecraft server
    */
   public boolean isMultiplayerMode() {
      return mc.isMultiplayerWorld();
   }

   /**
    * Gets all the usernames on the current server you're on
    *
    * @return A list in alphabetical order of players logged onto the server
    */
   public List<String> getPlayerNames() {
      List<String> names;
      if (isMultiplayerMode() && mc.thePlayer instanceof EntityClientPlayerMP) {
         names = new ArrayList<String>();
         NetClientHandler netclienthandler = ((EntityClientPlayerMP) mc.thePlayer).sendQueue;
         List tempList = netclienthandler.playerNames;
         for (GuiPlayerInfo info : (List<GuiPlayerInfo>) tempList) {
            String name = info.name; //There were some problems with bukkit plugins adding prefixes or suffixes to the names list. This cleans the strings.
            Pattern pattern = Pattern.compile("[\\[[\\{[\\(]]]+?.*?[\\][\\}[\\)]]]"); //Get rid of everything between the brackets (), [], or {}
            Matcher matcher = pattern.matcher(name);
            name = matcher.replaceAll("");
            String cleanName = "";
            for (int i = 0; i < name.length(); i++) { //Get rid of every invalid character for minecraft usernames
               if (name.charAt(i) == '�') { //Gets rid of color codes
                  i++;
                  continue;
               }

               if (Character.isLetterOrDigit(name.charAt(i)) || name.charAt(i) == '_') {
                  cleanName += name.charAt(i);
               }
            }
            if (!cleanName.equals("")) {
               names.add(cleanName);
            }
         }
      } else {
         names = null;
      }
      return names;
   }

   /**
    * Gets all possible autocomplete words, including playernames
    *
    * @return A list of all possible word completions for autoword
    */

   public List<String> getAutoPossibility()
   {
      List<String> autowords = new ArrayList<String>();
      List<String> players = getPlayerNames();
      if(players != null && players.size() > 0)
      {
         autowords.addAll(players);
      }

      autowords.addAll(ConsoleChatCommands.getChatCommands());

      return autowords;
   }

   /**
    * Changes the highlighting bounds so nothing is highlighted
    */

   public void clearHighlighting() {
      lastHighlighting[0] = -1;
      lastHighlighting[1] = 0;
      initialHighlighting[0] = -1;
      initialHighlighting[1] = 0;
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
         clean = clean.substring(0, INPUT_MAX - 1);
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
            return cleanString((String) t.getTransferData(DataFlavor.stringFlavor));
         }
      } catch (Exception e) {
      }
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
      } catch (Exception e) {
      }
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
    * Sets the inputOffset value to the appropriate place
    */

   private void validateOffset() {
      String start = message.substring(0, cursor);
      String end = message.substring(cursor, message.length());
      input = INPUT_PREFIX + start + ((updateCounter / 8) % 2 != 0 ? "." : "!") + end;

      if (fontRenderer.getStringWidth(input) >= TEXT_BOX[2] - TEXT_BOX[0] - BORDERSIZE * 2) {
         int upperbound = input.length();
         int boxsize = TEXT_BOX[2] - TEXT_BOX[0] - BORDERSIZE * 2;

         if (inputOffset < 0) {
            inputOffset = 0;
         }

         if (inputOffset > INPUT_PREFIX.length()) {
            while (cursor < inputOffset - INPUT_PREFIX.length() && inputOffset > 0) {
               inputOffset--;
            }
         } else {
            while (cursor < inputOffset && inputOffset > 0) {
               inputOffset--;
            }
         }

         while (fontRenderer.getStringWidth(input.substring(inputOffset, cursor + INPUT_PREFIX.length() + 1)) >= boxsize) {
            inputOffset++;
         }

         while (fontRenderer.getStringWidth(input.substring(inputOffset, upperbound)) >= boxsize) {
            upperbound--;
         }

         if (upperbound > input.length()) {
            upperbound = input.length();
         }
         input = input.substring(inputOffset, upperbound);
      }
   }

   /**
    * Makes sure the highlighting values are within the string bounds
    */
   private void validateHighlighting() {
      if (initialHighlighting[0] < -1) {
         initialHighlighting[0] = -1;
      } else if (initialHighlighting[0] > LINES.size()) {
         initialHighlighting[0] = LINES.size();
      }

      if (lastHighlighting[0] < -1) {
         lastHighlighting[0] = -1;
      } else if (lastHighlighting[0] > LINES.size()) {
         lastHighlighting[0] = LINES.size();
      }

      if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
         if (lastHighlighting[1] < 0) {
            lastHighlighting[1] = 0;
         } else if (lastHighlighting[1] > message.length()) {
            lastHighlighting[1] = message.length();
         }

         if (initialHighlighting[1] < 0) {
            initialHighlighting[1] = 0;
         } else if (initialHighlighting[1] > message.length()) {
            initialHighlighting[1] = message.length();
         }
      } else {
         if (lastHighlighting[1] < 0) {
            lastHighlighting[1] = 0;
         } else if (lastHighlighting[1] > LINES.get(lastHighlighting[0]).length()) {
            lastHighlighting[1] = LINES.get(lastHighlighting[0]).length();
         }

         if (initialHighlighting[1] < 0) {
            initialHighlighting[1] = 0;
         } else if (initialHighlighting[1] > LINES.get(initialHighlighting[0]).length()) {
            initialHighlighting[1] = LINES.get(initialHighlighting[0]).length();
         }
      }
   }

   /**
    * Paste clipboard at cursor position.
    */
   private void paste() {
      String clipboard = getClipboardString();
      if (clipboard != null) {
         if (lastHighlighting[1] == initialHighlighting[1] || initialHighlighting[0] != -1 || lastHighlighting[0] != -1) {
            String start = "";
            String end = "";
            validateCursor();

            if (message != null && message.length() > 0) {
               start = message.substring(0, cursor);
               end = message.substring(cursor);
            }

            int limit = INPUT_MAX - message.length();
            if (limit < clipboard.length()) {
               clipboard = clipboard.substring(0, limit);
            }

            message = start + clipboard + end;
            cursor = (start + clipboard).length() + 1;
         } else {
            String start, end;
            if (initialHighlighting[1] < lastHighlighting[1]) {
               start = message.substring(0, initialHighlighting[1]);
               end = message.substring(lastHighlighting[1]);
            } else {
               start = message.substring(0, lastHighlighting[1]);
               end = message.substring(initialHighlighting[1]);
            }

            int limit = INPUT_MAX - message.length();
            if (limit < clipboard.length()) {
               clipboard = clipboard.substring(0, limit);
            }

            message = start + clipboard + end;
            cursor = (start + clipboard).length() + 1;
            clearHighlighting();
         }
      }
   }

   /**
    * Delete the next character.
    */
   private void delete() {
      // Delete
      if (message.length() > 0) {
         if (initialHighlighting[1] == lastHighlighting[1] || initialHighlighting[0] != -1 || lastHighlighting[0] != -1) {
            validateCursor();
            String start = message.substring(0, cursor);
            String end = message.substring(cursor, message.length());
            this.message = start + (end.length() > 0 ? end.substring(1) : end);
         } else if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1) {
            String start, end;
            if (initialHighlighting[1] < lastHighlighting[1]) {
               start = message.substring(0, initialHighlighting[1]);
               end = message.substring(lastHighlighting[1]);
            } else {
               start = message.substring(0, lastHighlighting[1]);
               end = message.substring(initialHighlighting[1]);
            }
            message = start + end;
         }
         clearHighlighting();
      }
   }

   /**
    * Called per frame to draw the new frame
    *
    * @see net.minecraft.src.GuiScreen#drawScreen(int, int, float)
    */
   @Override
   public void drawScreen(int mousex, int mousey, float f) {
      // Background
      int minx = SCREEN_PADDING_LEFT;
      int miny = SCREEN_PADDING_TOP;
      int maxx = width - SCREEN_PADDING_RIGHT;
      int maxy = height - SCREEN_PADDING_BOTTOM;
      drawRect(minx, miny, maxx, maxy, COLOR_BASE);

      // Input Text box
      int textbox_minx = minx + BORDERSIZE;
      int textbox_maxx = maxx - BORDERSIZE;
      int textbox_miny = maxy - CHARHEIGHT - BORDERSIZE;
      int textbox_maxy = maxy - BORDERSIZE;
      drawRect(textbox_minx, textbox_miny, textbox_maxx, textbox_maxy, COLOR_INPUT_BACKGROUND);
      TEXT_BOX = new int[] { textbox_minx, textbox_miny, textbox_maxx, textbox_maxy };

      // Input text highlighting
      if (initialHighlighting[0] == -1 && lastHighlighting[0] == -1 && lastHighlighting[1] != initialHighlighting[1]) {
         int firstH, lastH; //First letter position, last letter position
         if (initialHighlighting[1] < lastHighlighting[1]) {
            firstH = initialHighlighting[1];
            lastH = lastHighlighting[1];
         } else {
            firstH = lastHighlighting[1];
            lastH = initialHighlighting[1];
         }

         if (firstH < 0) {
            firstH = 0;
         }

         if (lastH > message.length()) {
            lastH = message.length();
         }

         String messageSection;
         validateCursor();
         validateOffset();
         validateHighlighting();
         if (inputOffset < INPUT_PREFIX.length()) {
            messageSection = (INPUT_PREFIX + message.substring(0, firstH)).substring(inputOffset);
         } else {
            messageSection = message.substring((inputOffset - INPUT_PREFIX.length()) > firstH ? firstH : inputOffset - INPUT_PREFIX.length(), firstH);
         }

         int highlighting_minx = 1 + TEXT_BOX[0] + fontRenderer.getStringWidth(messageSection);

         if (firstH < inputOffset - INPUT_PREFIX.length()) {
            messageSection = message.substring(inputOffset - INPUT_PREFIX.length(), lastH);
         } else {
            messageSection = message.substring(firstH, lastH);
         }

         int highlighting_maxx = 1 + highlighting_minx + fontRenderer.getStringWidth(messageSection);
         int highlighting_miny = TEXT_BOX[1];
         int highlighting_maxy = highlighting_miny + CHARHEIGHT;
         if (cursor > firstH && cursor < lastH)
            highlighting_maxx += fontRenderer.getStringWidth("!");
         else if (cursor <= firstH) {
            highlighting_minx += fontRenderer.getStringWidth("!");
            highlighting_maxx += fontRenderer.getStringWidth("!");
         }

         if (highlighting_maxx > TEXT_BOX[2]) {
            highlighting_maxx = TEXT_BOX[2];
         }

         drawRect(highlighting_minx, highlighting_miny, highlighting_maxx, highlighting_maxy, COLOR_TEXT_HIGHLIGHT);
      }

      // Past messages - dialog
      int message_miny = miny + BORDERSIZE;
      int message_maxy = textbox_miny - BORDERSIZE;
      if (textbox_minx != MESSAGE_MINX || MESSAGE_MAXX != maxx - (BORDERSIZE * 2) - 10) {
         MESSAGE_MINX = textbox_minx;
         MESSAGE_MAXX = maxx - (BORDERSIZE * 2) - 10;
         buildLines();
      }

      HISTORY = new int[] { MESSAGE_MINX, message_miny, MESSAGE_MAXX, message_maxy };

      if (LINES == null || rebuildLines) {
         buildLines();
      }

      drawRect(MESSAGE_MINX, message_miny, MESSAGE_MAXX, message_maxy, COLOR_OUTPUT_BACKGROUND);

      // Past messages - highlighting

      if (initialHighlighting[0] > -1 && lastHighlighting[0] > -1 && !Arrays.equals(initialHighlighting, lastHighlighting)) {
         int maxDisplayedLines = (HISTORY[3] - HISTORY[1]) / (CHARHEIGHT - 1);
         int linesDisplayed = LINES.size() >= maxDisplayedLines ? maxDisplayedLines : LINES.size();
         int lineAtOnScreen_i;
         if (linesDisplayed < maxDisplayedLines) {
            lineAtOnScreen_i = maxDisplayedLines - LINES.size() + initialHighlighting[0];
         } else {
            lineAtOnScreen_i = initialHighlighting[0] - LINES.size() + linesDisplayed + slider;
         }

         int lineAtOnScreen_f;
         if (linesDisplayed < maxDisplayedLines) {
            lineAtOnScreen_f = maxDisplayedLines - LINES.size() + lastHighlighting[0];
         } else {
            lineAtOnScreen_f = lastHighlighting[0] - LINES.size() + linesDisplayed + slider;
         }

         int[] rect = new int[4];
         int xoffset = HISTORY[0] + BORDERSIZE;
         int yoffset = HISTORY[1] + BORDERSIZE;

         //initial
         int h_minx = xoffset + fontRenderer.getStringWidth(LINES.get(initialHighlighting[0]).substring(0, initialHighlighting[1])) - 1;
         int h_miny = yoffset + ((CHARHEIGHT - 1) * lineAtOnScreen_i) - 2;
         int h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(initialHighlighting[0]).substring(initialHighlighting[1])) + 2;
         int h_maxy = h_miny + CHARHEIGHT;

         if (lastHighlighting[0] != initialHighlighting[0]) {
            if (lastHighlighting[0] < initialHighlighting[0]) {
               h_maxx = xoffset - 1;
            }

            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);

            //inbetween
            int firstOnScreen = lineAtOnScreen_i <= lineAtOnScreen_f ? lineAtOnScreen_i : lineAtOnScreen_f;
            int firstInLINES = initialHighlighting[0] <= lastHighlighting[0] ? initialHighlighting[0] : lastHighlighting[0];

            for (int i = 1; i < Math.abs(lineAtOnScreen_i - lineAtOnScreen_f); i++) {
               h_minx = xoffset - 1;
               h_miny = yoffset + ((CHARHEIGHT - 1) * (firstOnScreen + i)) - 2;
               h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(firstInLINES + i)) + 2;
               h_maxy = h_miny + CHARHEIGHT;

               drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
            }

            //final
            h_minx = xoffset + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(0, lastHighlighting[1])) - 1;
            h_miny = yoffset + ((CHARHEIGHT - 1) * lineAtOnScreen_f) - 2;
            h_maxx = h_minx + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(lastHighlighting[1])) + 2;
            h_maxy = h_miny + CHARHEIGHT;

            if (lastHighlighting[0] > initialHighlighting[0]) {
               h_maxx = xoffset - 1;
            }

            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
         } else {
            h_maxx = xoffset + fontRenderer.getStringWidth(LINES.get(lastHighlighting[0]).substring(0, lastHighlighting[1])) - 1;
            drawRect(h_minx, h_miny, h_maxx, h_maxy, COLOR_TEXT_HIGHLIGHT);
         }
      }

      // Past messages - text
      int max = (message_maxy - message_miny) / (CHARHEIGHT - 1);
      if (slider != 0) {
         slider = LINES.size() - slider > max ? (LINES.size() - slider < LINES.size() ? slider : 0) : LINES.size() - max;
      }

      if (slider < 0)
         slider = 0;

      int oversize = 0;
      for (int i = 0; i + oversize < LINES.size() && i + oversize < max; i++) {
         int element = LINES.size() - 1 - i - slider;
         if (LINES.size() <= element)
            continue;
         drawString(this.mc.fontRenderer, LINES.elementAt(element), MESSAGE_MINX + BORDERSIZE, textbox_miny - CHARHEIGHT + 1 - BORDERSIZE - ((i + oversize) * (CHARHEIGHT - 1)), COLOR_TEXT_OUTPUT);
      }

      // Scroll - background
      int scroll_minx = MESSAGE_MAXX + BORDERSIZE;
      int scroll_maxx = textbox_maxx;
      int scroll_miny = message_miny;
      int scroll_maxy = textbox_miny - BORDERSIZE;
      drawRect(scroll_minx, scroll_miny, scroll_maxx, scroll_maxy, COLOR_SCROLL_BACKGROUND);

      // Scroll - button top
      drawRect(scroll_minx + 1, scroll_miny + 1, scroll_maxx - 1, scroll_miny + 9, COLOR_SCROLL_FOREGROUND);
      TOP = new int[] { scroll_minx + 1, scroll_miny + 1, scroll_maxx - 1, scroll_miny + 9 };
      drawString(this.mc.fontRenderer, "^", TOP[0] + 2, TOP[1] + 2, COLOR_SCROLL_ARROW);

      // Scroll - button bottom
      drawRect(scroll_minx + 1, scroll_maxy - 9, scroll_maxx - 1, scroll_maxy - 1, COLOR_SCROLL_FOREGROUND);
      BOTTOM = new int[] { scroll_minx + 1, scroll_maxy - 9, scroll_maxx - 1, scroll_maxy - 1 };
      drawStringFlipped(this.mc.fontRenderer, "^", BOTTOM[0] + 1, BOTTOM[1] - 3, COLOR_SCROLL_ARROW, true);

      // Scroll - bar
      int scrollable_minx = scroll_minx + 1;
      int scrollable_maxx = scroll_maxx - 1;
      int scrollable_miny = scroll_miny + 11;
      int scrollable_maxy = scroll_maxy - 10;
      sliderHeight = scrollable_maxy - scrollable_miny;
      double heightpercentage = (double) max / (double) LINES.size();
      double barheight = (sliderHeight) * heightpercentage;
      barheight = barheight < 5 ? 5 : barheight;
      double stepsize = (sliderHeight - barheight) / (double) (LINES.size() - max);
      double position = slider * stepsize;

      if (LINES.size() < max) {
         drawRect(scrollable_minx, scrollable_miny, scrollable_maxx, scrollable_maxy, COLOR_SCROLL_FOREGROUND);
         BAR = new int[] { scrollable_minx, scrollable_miny, scrollable_maxx, scrollable_maxy };
      } else {
         drawRect(scrollable_minx, (int) (scrollable_maxy - position - barheight), scrollable_maxx, (int) (scrollable_maxy - position), COLOR_SCROLL_FOREGROUND);
         BAR = new int[] { scrollable_minx, (int) (scrollable_maxy - position - barheight), scrollable_maxx, (int) (scrollable_maxy - position) };
      }

      // Input
      validateCursor();
      validateOffset();
      drawString(this.mc.fontRenderer, input, textbox_minx + BORDERSIZE, textbox_miny + 1, COLOR_INPUT_TEXT);

      // Titlebar
      drawRect(maxx / 2, 0, maxx, miny, COLOR_BASE);

      // Title
      drawString(this.mc.fontRenderer, TITLE, (maxx / 2) + BORDERSIZE, BORDERSIZE, COLOR_TEXT_TITLE);

      // Exit button
      drawRect(maxx - BORDERSIZE - 10, BORDERSIZE, maxx - BORDERSIZE, miny, COLOR_EXIT_BUTTON);
      drawString(this.mc.fontRenderer, "X", maxx - BORDERSIZE - 7, BORDERSIZE + 2, COLOR_EXIT_BUTTON_TEXT);
      EXIT = new int[] { maxx - BORDERSIZE - 10, BORDERSIZE, maxx - BORDERSIZE, miny };

      super.drawScreen(mousex, mousey, f);
   }

   /**
    * Draws the specified String flipped upside down
    *
    * @param fontrenderer
    * @param s string to draw
    * @param i position of the x coordinate
    * @param j position of the y coordinate
    * @param k colour of the render
    * @param flag if true draw with shadow, if false draw without shadow
    */
   public void drawStringFlipped(FontRenderer fontrenderer, String s, int i, int j, int k, boolean flag) {
      GL11.glPushMatrix();
      GL11.glScalef(-1F, -1F, 1F);
      GL11.glTranslatef((-i * 2) - fontrenderer.getStringWidth(s), (-j * 2) - fontrenderer.FONT_HEIGHT, 0.0F);
      if (flag) {
         fontrenderer.drawString(s, i - 1, j - 1, (k & 0xfcfcfc) >> 2 | k & 0xff000000); //Took the last argument from FrontRenderer.renderString() because it's private and I want the shadow on the correct side when flipped
      }
      fontrenderer.drawString(s, i, j, k);
      GL11.glPopMatrix();
   }

   /**
    * Tests whether the (x, y) coordinate is within the rectangle or not
    *
    * @param x x coordinate
    * @param y y coordinate
    * @param rect integer array in the form of {mix x, min y, max x, max y}
    * @return true if point is in rect false if point is not in rect
    */

   public boolean hitTest(int x, int y, int[] rect) {
      if (x >= rect[0] && x <= rect[2] && y >= rect[1] && y <= rect[3])
         return true;
      else
         return false;
   }

   /**
    * Returns the mouse position as an index within the string line
    *
    * @param x The mouse's x position relative to the start of the string line
    * @param line The string to find the index in
    * @return the character index the mouse clicked at.
    */

   public int mouseAt(int x, String line) {
      int left = 0;
      int right = line.length();

      if (x >= fontRenderer.getStringWidth(line)) {
         return line.length();
      }

      while (left <= right) {
         int middle = (left + right) / 2;
         int length = fontRenderer.getStringWidth(line.substring(0, middle));
         double upper, lower;
         if (middle < line.length() - 1) {
            upper = length + (fontRenderer.getStringWidth(Character.toString(line.charAt(middle))) / 2.0);
         } else {
            upper = fontRenderer.getStringWidth(line);
         }

         if (middle >= 1) {
            lower = length - (fontRenderer.getStringWidth(Character.toString(line.charAt(middle - 1))) / 2.0);
         } else {
            lower = 0;
         }

         if ((x <= upper && x >= lower)) {
            return middle;
         } else if (x < lower) {
            right = middle - 1;
         } else if (x > upper) {
            left = middle + 1;
         }
      }

      return 0;
   }

   /**
    * Called on mouse clicked and processes the button clicks and actions
    *
    * @see net.minecraft.src.GuiScreen#mouseClicked(int, int, int)
    */
   @Override
   protected void mouseClicked(int mousex, int mousey, int button) {
      if (button == 0) {
         // Bad implementation which checks for clicks on exit button
         if (hitTest(mousex, mousey, EXIT)) {
            mc.displayGuiScreen(null);
            return;
            // Bad implementation which checks for clicks on scrollbar
         } else if (hitTest(mousex, mousey, TOP))
            slider++;
         else if (hitTest(mousex, mousey, BOTTOM))
            slider--;
         else if (hitTest(mousex, mousey, BAR)) {
            isSliding = true;
            lastSliding = mousey;
            initialSliding = slider;
         } else if (hitTest(mousex, mousey, TEXT_BOX)) {
            tabPosition = 0;
            isHighlighting = true;
            initialHighlighting[0] = lastHighlighting[0] = -1;
            int mousexCorrected = mousex - TEXT_BOX[0] - BORDERSIZE;
            int startStringIndex = 0;
            int cutPrefixChars = (inputOffset <= INPUT_PREFIX.length() ? inputOffset : INPUT_PREFIX.length());
            if (inputOffset < INPUT_PREFIX.length()) {
               mousexCorrected -= fontRenderer.getStringWidth(INPUT_PREFIX.substring(inputOffset));
            } else {
               startStringIndex = inputOffset - INPUT_PREFIX.length();
            }

            if (mousexCorrected > fontRenderer.getStringWidth(message.substring(startStringIndex, cursor) + "!")) {
               mousexCorrected -= fontRenderer.getStringWidth("!");
            }

            int charat = mouseAt(mousexCorrected, message.substring(startStringIndex)) + startStringIndex;
            if (message.length() < charat)
               initialHighlighting[1] = message.length();
            else
               initialHighlighting[1] = charat;
            cursor = lastHighlighting[1] = initialHighlighting[1];
         } else if (hitTest(mousex, mousey, HISTORY)) {
            tabPosition = 0;
            isHighlighting = true;
            int mousexCorrected = mousex - HISTORY[0] - BORDERSIZE;
            int lineAt = correctYlineAt(mousey);
            initialHighlighting[0] = lineAt;
            int charAt = mouseAt(mousexCorrected, LINES.get(lineAt));
            initialHighlighting[1] = charAt;
         }

         super.mouseClicked(mousex, mousey, button);
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
         slider += wheel / 120 * LINES_PER_SCROLL;
      }

      // Moves the slider position
      if (isSliding) {
         if (Mouse.isButtonDown(0)) {
            int diff = lastSliding - mousey;
            if (diff != 0) {
               slider = initialSliding + (int) ((diff / (double) sliderHeight) * (LINES.size()));
            }
         } else {
            isSliding = false;
            lastSliding = 0;
            initialSliding = 0;
         }
      } else if (isHighlighting) {
         if (hitTest(mousex, mousey, TEXT_BOX) || initialHighlighting[0] == -1) {
            int mousexCorrected = mousex - TEXT_BOX[0] - BORDERSIZE;
            int startStringIndex = 0;

            if (inputOffset < INPUT_PREFIX.length()) {
               mousexCorrected -= fontRenderer.getStringWidth(INPUT_PREFIX.substring(inputOffset));
            } else {
               startStringIndex = inputOffset - INPUT_PREFIX.length();
            }

            if (mousexCorrected > fontRenderer.getStringWidth(message.substring(startStringIndex, cursor) + "!")) {
               mousexCorrected -= fontRenderer.getStringWidth("!");
            }

            int charat = mouseAt(mousexCorrected, message.substring(startStringIndex)) + startStringIndex;
            if (charat < 0) {
               charat = 0;
            }
            if (message.length() < charat) {
               lastHighlighting[1] = message.length();
            } else {
               lastHighlighting[1] = charat;
            }
            if (initialHighlighting[0] == lastHighlighting[0] && initialHighlighting[0] == -1) {
               cursor = lastHighlighting[1];
               validateCursor();
            }
            validateOffset();
         } else if (hitTest(mousex, mousey, HISTORY)) {
            tabPosition = 0;
            isHighlighting = true;
            int mousexCorrected = mousex - HISTORY[0] - BORDERSIZE;
            int lineAt = correctYlineAt(mousey);
            lastHighlighting[0] = lineAt;
            int charAt = mouseAt(mousexCorrected, LINES.get(lineAt));
            lastHighlighting[1] = charAt;
         }
         if (!Mouse.isButtonDown(0)) {
            isHighlighting = false;
         }
      }
   }

   private int correctYlineAt(int mousey)
   {
      int maxDisplayedLines = (HISTORY[3] - HISTORY[1]) / (CHARHEIGHT - 1);
      int linesDisplayed = LINES.size() >= maxDisplayedLines ? maxDisplayedLines : LINES.size();
      int mouseyCorrected = mousey - HISTORY[1] - BORDERSIZE;
      int lineAt = mouseyCorrected / (CHARHEIGHT - 1) + LINES.size() - linesDisplayed - slider;

      if (lineAt >= LINES.size()) {
         lineAt = LINES.size() - maxDisplayedLines + lineAt;
         if (lineAt < 0) {
            lineAt = 0;
         }
      }

      return lineAt;
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

      boolean post = true;
      for (ConsoleListener cl : LISTENERS) {
         if (!cl.processInput(message)) {
            post = false;
         }
      }

      if (post) {
         if (isMultiplayerMode()) { // Verifies that each line doesn't pass the maximum server length
            int lastLen = 0;
            for (int i = 0; i <= message.length() / INPUT_SERVER_MAX; i++) {
               int end = (lastLen + INPUT_SERVER_MAX) > message.length() ? message.length() : (lastLen + INPUT_SERVER_MAX);
               if (message.length() > INPUT_SERVER_MAX && message.substring(lastLen, end).length() >= INPUT_SERVER_MAX) {
                  for (int j = 1; j <= 10; j++) {
                     if (message.charAt(end - j) == ' ') { //Wrap at space if it's within 10 characters
                        end = end - j;
                        break;
                     }
                  }
               }

               mc.thePlayer.sendChatMessage(message.substring(lastLen, end));

               if (message.length() == INPUT_SERVER_MAX) {
                  break; //Fix for displaying an extra line when length is exactly at the limit
               }
               lastLen = end;
            }
         } else {
            mc.thePlayer.sendChatMessage(message);
         }
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

   /**
    * Grabs to IN GAME GUI list of messages for the console mod. This allows
    * the mod to display what goes onto the inbuild message line without
    * needing to change core classes.
    */
   private void pullGuiList() {
      try {
         if (getInGameGuiList() && IN_GAME_GUI.size() > 0 && !((ChatLine) IN_GAME_GUI.get(0)).equals(lastOutput)) {
            int pointer = 0;
            while (pointer < IN_GAME_GUI.size()) {
               if (((ChatLine) IN_GAME_GUI.get(pointer)).equals(lastOutput)) {
                  break;
               }
               pointer++;
            }
            --pointer;
            for (int i = pointer; i > -1; i--) {
               addOutputMessage(((ChatLine) IN_GAME_GUI.get(i)).message);
            }
            lastOutput = (ChatLine) IN_GAME_GUI.get(0);
         }
      } catch (Exception e) {
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
                  FileOutputStream fos = new FileOutputStream(new File(LOG_DIR, logName), true);
                  while (log.size() > 0) {
                     String line = sdf.format(new Date()) + log.elementAt(0) + LINE_BREAK;
                     fos.write(line.getBytes());
                     log.remove(0);
                  }
                  lastWrite = System.currentTimeMillis();
               } catch (FileNotFoundException e) {
               }
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
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, property);
                     }
                  } else if (field.getType().equals(Integer.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Integer.decode(property).intValue());
                     }
                  } else if (field.getType().equals(Double.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Double.parseDouble(property));
                     }
                  } else if (field.getType().equals(Boolean.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Boolean.parseBoolean(property));
                     }
                  } else if (field.getType().equals(Long.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Long.decode(property).longValue());
                     }
                  } else if (field.getType().equals(Byte.TYPE)) { // new
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Byte.parseByte(property));
                     }
                  } else if (field.getType().equals(Float.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Float.parseFloat(property));
                     }
                  } else if (field.getType().equals(Short.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, Short.decode(property));
                     }
                  } else if (field.getType().equals(Character.TYPE)) {
                     String property = (String) p.get(field.getName());
                     if (property != null) {
                        field.set(null, property.charAt(0));
                     }
                  }
               } catch (Exception e) {
               }
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
         } catch (Exception e) {
         }
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
                     p.setProperty(field.getName(), (String) field.get(null));
                  } else if (field.getType().equals(Byte.TYPE)) {
                     p.setProperty(field.getName(), field.getByte(null) + "");
                  } else if (field.getType().equals(Short.TYPE)) {
                     p.setProperty(field.getName(), field.getShort(null) + "");
                  } else if (field.getType().equals(Float.TYPE)) {
                     p.setProperty(field.getName(), field.getFloat(null) + "");
                  } else if (field.getType().equals(Character.TYPE)) {
                     p.setProperty(field.getName(), field.getChar(null) + "");
                  }
               } catch (Exception e) {
               }
            }
         }
         p.store(new FileOutputStream(settings), "");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
