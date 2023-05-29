/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package parbft.reconfiguration.views;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author eduardo
 */
public class View implements Serializable {
	
	private static final long serialVersionUID = 4052550874674512359L;
	
	private int id;
 	private int f;
 	private int[] processes;
	private int[] group;
	private Map<Integer,GroupNet> addresses;

	public View(int id, int[] processes, int f, InetSocketAddress[] addresses,int[] group){
 		this.id = id;
 		this.processes = processes;
		this.addresses = new HashMap<Integer, GroupNet>();
		this.group = group;

 		for(int i = 0; i < this.processes.length;i++)
			this.addresses.put(processes[i], new GroupNet(processes[i], group[i], addresses[i]));
 		Arrays.sort(this.processes);
 		this.f = f;
 	}

 	public boolean isMember(int id){
 		for(int i = 0; i < this.processes.length;i++){
 			if(this.processes[i] == id){
 				return true;
 			}
 		}
 		return false;
 	}


// 	public int getPos(int id){
// 		for(int i = 0; i < this.processes.length;i++){
// 			if(this.processes[i] == id){
// 				return i;
// 			}
// 		}
// 		return -1;
// 	}
	public int getPos(int id){
		int num = -1;
		int group = addresses.get(id).getGroup();
		for(int i = 0; i < this.processes.length;i++){
			if (addresses.get(processes[i]).group==group)
				num++;
			if(this.processes[i] == id){
				return num;
			}
		}
		return -1;
	}

 	public int getId() {
 		return id;
 	}

 	public int getF() {
 		return f;
 	}

// 	public int getN(){
// 		return this.processes.length;
// 	}
	public int getN(int group){
		int num = 0;
		for(int i = 0 ; i < processes.length;i++){
			if(addresses.get(i).getGroup() == group){
				num++;
			}
		}
		return  num;
	}

 	public int[] getProcesses() {
 		return processes;
 	}

	public int[] getGroupProcesses(int group) {
		ArrayList<Integer> groupProcesses = new ArrayList<>();
		for(int i = 0 ; i < processes.length;i++){
			if(addresses.get(i).getGroup() == group){
				groupProcesses.add(addresses.get(i).getProcesses());
			}
		}
		return  groupProcesses.stream().mapToInt(x->x).toArray();
	}
	@Override
	public String toString(){
		String ret = "ID:"+id+"; F:"+f+"; Processes:";
		int flag = -1;
		for(int i = 0; i < processes.length;i++){
			if(addresses.get(processes[i]).getGroup()!=flag){
				flag = addresses.get(processes[i]).getGroup();
				ret = ret+"\n\tGroup "+addresses.get(processes[i]).getGroup()+":";
			}
			ret = ret+"["+processes[i]+"("+addresses.get(processes[i]).getAddress()+")],";
		}

		return ret;
	}
	public InetSocketAddress getAddress(int id) {
		return addresses.get(id).getAddress();
	}
        
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof View) {
            View v = (View) obj;
//            return (this.addresses.equals(v.addresses) &&
            return (this.addresses.keySet().equals(v.addresses.keySet()) &&
                    Arrays.equals(this.processes, v.processes)
                    && this.id == v.id && this.f == v.f);
            
        }
        return false;
    }
    
    public int hashCode() {
        int hash = 1;
        hash = hash * 31 + this.id;
        hash = hash * 31 + this.f;
        if (this.processes != null) {
            for (int i = 0; i < this.processes.length; i++) hash = hash * 31 + this.processes[i];
        } else {
            hash = hash * 31 + 0;
        }
        hash = hash * 31 + this.addresses.hashCode();
        return hash;
    }
	public static class GroupNet implements Serializable{

		private int processes;
		private int group;
		private InetSocketAddress address;

		public GroupNet(int processes,int group, InetSocketAddress address) {
			this.processes = processes;
			this.group = group;
			this.address = address;
		}

		public int getProcesses() {
			return processes;
		}

		public void setProcesses(int processes) {
			this.processes = processes;
		}

		public int getGroup() {
			return group;
		}

		public void setGroup(int group) {
			this.group = group;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public void setAddress(InetSocketAddress address) {
			this.address = address;
		}
	}
}
