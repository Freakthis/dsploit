/*
 * This file is part of the dSploit.
 *
 * Copyleft of Simone Margaritelli aka evilsocket <evilsocket@gmail.com>
 *
 * dSploit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dSploit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dSploit.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.evilsocket.dsploit.plugins;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CheckBox;

import it.evilsocket.dsploit.R;
import it.evilsocket.dsploit.core.Plugin;
import it.evilsocket.dsploit.core.System;
import it.evilsocket.dsploit.net.Network;
import it.evilsocket.dsploit.net.Target;
import it.evilsocket.dsploit.net.Target.Port;
import it.evilsocket.dsploit.tools.NMap.InspectionReceiver;

public class Inspector extends Plugin{

  private ToggleButton mStartButton = null;
  private ProgressBar mActivity = null;
  private TextView mDeviceType = null;
  private TextView mDeviceOS = null;
  private TextView mDeviceServices = null;
  private boolean mRunning = false;
  private Receiver mReceiver = null;
  private CheckBox mAdvancedScan = null;

  public Inspector(){
    super(
      R.string.inspector,
      R.string.inspector_desc,

      new Target.Type[]{Target.Type.ENDPOINT, Target.Type.REMOTE},
      R.layout.plugin_inspector,
      R.drawable.action_inspect
    );
  }

  private void setStoppedState(){
    System.getNMap().kill();
    mActivity.setVisibility(View.INVISIBLE);
    mRunning = false;
    mStartButton.setChecked(false);
  }

  private void write_services()
  {
    mDeviceServices.setText("");
    if(System.getCurrentTarget().hasOpenPortsWithService()) {
      for(Port port : System.getCurrentTarget().getOpenPorts()) {
        if( port.service!=null && !port.service.isEmpty() )
          if( port.version!=null && !port.version.isEmpty() )
            mDeviceServices.append( port.number + " ( " + port.protocol.toString() + " ) : " + port.service + " - " + port.version +  "\n" );
          else
            mDeviceServices.append( port.number + " ( " + port.protocol.toString() + " ) : " + port.service + "\n" );
      }
    }
    else
      mDeviceServices.setText("unknown");
  }

  private void setStartedState(){
    mActivity.setVisibility(View.VISIBLE);
    mRunning = true;

    write_services();

    boolean AdvancedScan = mAdvancedScan.isChecked();
    Target target = System.getCurrentTarget();

    if( AdvancedScan && target.getOpenPorts().size() == 0 ){
      setStoppedState();
      Toast.makeText(this, "No open ports found, run the port scanner before selecting a focused scan.", Toast.LENGTH_LONG).show();
    }
    else
      System.getNMap().inpsect( target, mReceiver, AdvancedScan ).start();
  }

  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    mStartButton = (ToggleButton) findViewById(R.id.inspectToggleButton);
    mActivity = (ProgressBar) findViewById(R.id.inspectActivity);
    TextView mDeviceName = (TextView) findViewById(R.id.deviceName);
    mDeviceType = (TextView) findViewById(R.id.deviceType);
    mDeviceOS = (TextView) findViewById(R.id.deviceOS);
    mDeviceServices = (TextView) findViewById(R.id.deviceServices);
    mAdvancedScan = (CheckBox)findViewById(R.id.advancedScan);

    mDeviceName.setText(System.getCurrentTarget().toString());

    if(System.getCurrentTarget().getDeviceType() != null)
      mDeviceType.setText(System.getCurrentTarget().getDeviceType());

    if(System.getCurrentTarget().getDeviceOS() != null)
      mDeviceOS.setText(System.getCurrentTarget().getDeviceOS());

    write_services();
    mAdvancedScan.setEnabled(System.getCurrentTarget().hasOpenPorts());
    mAdvancedScan.setClickable(mAdvancedScan.isEnabled());

    mStartButton.setOnClickListener(new OnClickListener(){
      @Override
      public void onClick(View v){
        if(mRunning){
          setStoppedState();
        } else{
          setStartedState();
        }
      }
    }
    );

    mReceiver = new Receiver();
  }

  @Override
  public void onBackPressed(){
    setStoppedState();
    super.onBackPressed();
  }

  private class Receiver extends InspectionReceiver{
    @Override
    public void onServiceFound( final int port, final String protocol, final String service, final String version ){
      final boolean hasServiceDescription = !service.trim().isEmpty();
      final boolean hasVersion = (version != null && !version.isEmpty());

      Inspector.this.runOnUiThread(new Runnable(){
        @SuppressWarnings("ConstantConditions")
        @Override
        public void run(){
          if(mDeviceServices.getText().equals("unknown"))
            mDeviceServices.setText("");

          if(hasServiceDescription){
            if(hasVersion)
              mDeviceServices.append( port + " ( " + protocol + " ) : " + service + " - v" + version +  "\n" );
            else
              mDeviceServices.append( port + " ( " + protocol + " ) : " + service + "\n" );
          }
          else
            mDeviceServices.append(port + " ( " + protocol + " )\n");
        }
      });

      if(hasServiceDescription){
        if(hasVersion)
          System.addOpenPort( port, Network.Protocol.fromString(protocol), service, version );
        else
          System.addOpenPort( port, Network.Protocol.fromString(protocol), service );
      }
      else
        System.addOpenPort(port, Network.Protocol.fromString(protocol));
    }

    @Override
    public void onOpenPortFound(int port, String protocol){
      System.addOpenPort(port, Network.Protocol.fromString(protocol));
    }

    @Override
    public void onOsFound(final String os){
      System.getCurrentTarget().setDeviceOS(os);

      Inspector.this.runOnUiThread(new Runnable(){
        @Override
        public void run(){
          mDeviceOS.setText(os);
        }
      });
    }

    @Override
    public void onGuessOsFound(final String os){
      System.getCurrentTarget().setDeviceOS(os);

      Inspector.this.runOnUiThread(new Runnable(){
        @Override
        public void run(){
          mDeviceOS.setText(os);
        }
      });
    }

    @Override
    public void onDeviceFound(final String device){
      System.getCurrentTarget().setDeviceType(device);

      Inspector.this.runOnUiThread(new Runnable(){
        @Override
        public void run(){
          mDeviceType.setText(device);
        }
      });
    }

    @Override
    public void onServiceInfoFound(final String info){
      System.getCurrentTarget().setDeviceOS(info);

      Inspector.this.runOnUiThread(new Runnable(){
        @Override
        public void run(){
          mDeviceOS.setText(info);
        }
      });
    }

    @Override
    public void onEnd(int code){
      Inspector.this.runOnUiThread(new Runnable(){
        @Override
        public void run(){
          if(mRunning)
            setStoppedState();
        }
      });
    }
  }
}
