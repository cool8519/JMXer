package dal.tool.trace.jmxer.cli.data;

import java.util.List;

public class Tree<E> {

	private TreeNode<E> root;
	private TreeNode<E> current;
	

	public Tree() {
		this.root = null;
		this.current = null;
	}

	public Tree(E data) {
		this.root = new TreeNode<E>(data);
		this.current = root;
	}

	public Tree(TreeNode<E> root) {
		this.root = root;
		this.current = root;
	}
	
	public void setRoot(E data) {
		this.root = new TreeNode<E>(data);
	}
	
	public void setRoot(TreeNode<E> root) {
		this.root = root;
	}

	public void setCurrent(E data) {
		this.current = new TreeNode<E>(data);
	}

	public void setCurrent(TreeNode<E> current) {
		this.current = current;
	}

	public TreeNode<E> getRoot() {
		return root;
	}

	public TreeNode<E> getCurrent() {
		return current;
	}

	public List<TreeNode<E>> getLeafs() {
		return root.getLeafs();
	}
	
	public void toRoot() {
		current = root;
	}

	public int getMaxDepth() {
		int max = -1;
		for(TreeNode<E> leaf : getLeafs()) {
			max = Math.max(max, leaf.getDepth());
		}
		return max;
	}
	
}
