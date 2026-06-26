package dal.tool.cli;

import java.util.LinkedList;
import java.util.List;

import dal.tool.cli.command.Command;

/**
 * 명령어 실행 히스토리 관리
 * @author 권영달
 *
 */
public class History {

	
	protected static final int DEFAULT_MAX_SIZE = 50;

	protected int maxSize;
	protected LinkedList<Command> history;

	
	public History() {
		history = new LinkedList<Command>();
		setMaxSize(DEFAULT_MAX_SIZE);
	}
	
	public History(int maxSize) {
		history = new LinkedList<Command>();
		setMaxSize(maxSize);
	}
	
	public void setMaxSize(int maxSize) {
		if(maxSize > -1 && maxSize <= Integer.MAX_VALUE) {
			this.maxSize = maxSize;
		} else {
			this.maxSize = DEFAULT_MAX_SIZE;
		}
	}
	
	public int getMaxSize() {
		return this.maxSize;
	}
	
	
    protected boolean add(Command command) {
        if(!command.saveToHistory() || command == null || command.getCommandLine() == null || command.getCommandLine().trim().equals("")) {
        	return false;
        }
    	if(history.size() == maxSize) {
    		history.removeFirst();
    	}
    	history.addLast(command);
    	return true;
	}
	

    public List<Command> getHistoryList() {
    	return history;
    }
    
    
    public Command getLast() {
    	return history.getLast();
    }
    
    
    public Command get(int index) {
    	return history.get(index-1);
    }
    
    
    public int size() {
    	return history.size();
    }
    
    
    public void clear() {
    	history.clear();
    }
    
}
