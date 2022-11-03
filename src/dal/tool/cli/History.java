package dal.tool.cli;

import java.util.LinkedList;
import java.util.List;

import dal.tool.cli.command.Command;

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
	
	/**
	 * 명령어 히스토리 최대 저장개수를 설정한다.
	 * @param maxSize 최대 저장개수
	 */
	public void setMaxSize(int maxSize) {
		if(maxSize > -1 && maxSize <= Integer.MAX_VALUE) {
			this.maxSize = maxSize;
		} else {
			this.maxSize = DEFAULT_MAX_SIZE;
		}
	}
	
	/**
	 * 명령어 히스토리 최대 저장개수를 가져온다.
	 * @return 최대 저장개수
	 */
	public int getMaxSize() {
		return this.maxSize;
	}
	
	
    /**
     * 히스토리에 명령어를 추가한다.
     * @param command 명령어
     * @return 명령어가 추가되었으면 true.
     */
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
	

    /**
     * 명령어 히스토리 목록을 가져온다.
     * @return 명령어 히스토리 목록
     */
    public List<Command> getHistoryList() {
    	return history;
    }
    
    
    /**
     * 마지막 명령어를 가져온다.
     * @return 마지막 명령어 객체
     */
    public Command getLast() {
    	return history.getLast();
    }
    
    
    /**
     * 명령어를 가져온다.
     * @param index 히스토리 목록의 순서(1부터 시작)
     * @return 명령어 객체
     */
    public Command get(int index) {
    	return history.get(index-1);
    }
    
    
    /**
     * 명령어 히스토리 갯수를 가져온다.
     * @return 히스토리 갯수
     */
    public int size() {
    	return history.size();
    }
    
    
    /**
     * 전체 명령어 히스토리를 제거한다.
     */
    public void clear() {
    	history.clear();
    }
    
}
