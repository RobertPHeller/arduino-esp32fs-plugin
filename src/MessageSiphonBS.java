/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-06 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.esp32.dlspiffs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;

/**
 * Slurps up messages from compiler.
 */
public class MessageSiphonBS implements Runnable {

  private final Reader streamReader;

  private Thread thread;
  private boolean canRun;
  // Data is processed line-by-line if possible, but if this is non-zero
  // then a partial line is also processed if no line end is received
  // within this many milliseconds.
  private int lineTimeout;

  public MessageSiphonBS(InputStream stream) {
    this(stream, 0);
  }

  public MessageSiphonBS(InputStream stream, int lineTimeout) {
    this.streamReader = new InputStreamReader(stream);
    this.canRun = true;
    this.lineTimeout = lineTimeout;

    thread = new Thread(this);
    thread.setName("MessageSiphonBS");
    // don't set priority too low, otherwise exceptions won't
    // bubble up in time (i.e. compile errors have a weird delay)
    //thread.setPriority(Thread.MIN_PRIORITY);
    thread.setPriority(Thread.MAX_PRIORITY - 1);
    thread.start();
  }


  @Override
  public void run() {
    try {
      // process data until we hit EOF; this will happily block
      // (effectively sleeping the thread) until new data comes in.
      // when the program is finally done, null will come through.
      //
      long lineStartTime = 0;
      boolean bsSeen = false;
      boolean beginningOfLine = true;  
      while (canRun) {
        // First, try to read as many characters as possible. Take care
        // not to block when:
        //  1. lineTimeout is nonzero, and
        //  2. we have some characters buffered already
        while (lineTimeout == 0 || beginningOfLine || streamReader.ready()) {
          int c = streamReader.read();
          if (c == -1)
            return; // EOF
          if (!canRun)
            return;

          // Keep track of the line start time
          if (beginningOfLine)
            lineStartTime = System.nanoTime();
            
          if (c == '\b') {
              if (!bsSeen) {
                  c = '\n';
                  bsSeen = true;
                  System.out.print((char )c);
                  beginningOfLine = true;
              }
          } else {
              bsSeen = false;
              System.out.print((char )c);
              beginningOfLine = (c == '\n');
          }

        }

        // No more characters available. Wait until lineTimeout
        // milliseconds have passed since the start of the line and then
        // try reading again. If the time has already passed, then just
        // pass on the characters read so far.
        long passed = (System.nanoTime() - lineStartTime) / 1000;
          //System.err.println("*** MessageSiphonBS.run(): passed = "+passed);
        if (passed < this.lineTimeout) {
          Thread.sleep(this.lineTimeout - passed);
          continue;
        }

      }
      //EditorConsole.systemOut.println("messaging thread done");
    } catch (NullPointerException npe) {
      // Fairly common exception during shutdown
    } catch (SocketException e) {
      // socket has been close while we were wainting for data. nothing to see here, move along
    } catch (Exception e) {
      // On Linux and sometimes on Mac OS X, a "bad file descriptor"
      // message comes up when closing an applet that's run externally.
      // That message just gets supressed here..
      String mess = e.getMessage();
      if ((mess != null) &&
              (mess.indexOf("Bad file descriptor") != -1)) {
        //if (e.getMessage().indexOf("Bad file descriptor") == -1) {
        //System.err.println("MessageSiphon err " + e);
        //e.printStackTrace();
      } else {
        e.printStackTrace();
      }
    } finally {
      thread = null;
    }
  }

  // Wait until the MessageSiphon thread is complete.
  public void join() throws java.lang.InterruptedException {
    // Grab a temp copy in case another thread nulls the "thread" 
    // member variable
    Thread t = thread;
    if (t != null) t.join();
  }

  public void stop() {
    this.canRun = false;
  }

}
