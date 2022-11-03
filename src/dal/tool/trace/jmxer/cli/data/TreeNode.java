package dal.tool.trace.jmxer.cli.data;

import java.util.ArrayList;
import java.util.List;

public class TreeNode<E> {

	private E data;
	private TreeNode<E> parent;
	private TreeNode<E> child;
	private TreeNode<E> sibling;
	private int depth;

	public TreeNode(E data) {
		this.data = data;
		this.parent = null;
		this.child = null;
		this.sibling = null;
		this.depth = 0;
	}

	public TreeNode(E data, int depth) {
		this.data = data;
		this.parent = null;
		this.child = null;
		this.sibling = null;
		this.depth = depth;
	}

	public TreeNode(E data, TreeNode<E> parent) {
		this.data = data;
		this.parent = parent;
		this.child = null;
		this.sibling = null;
		this.depth = parent.depth+1;
	}

	public void setData(E data) {
		this.data = data;
	}

	public E getData() {
		return data;
	}
	
	public TreeNode<E> getParent() {
		return parent;
	}
	
	public int getDepth() {
		return depth;
	}

	public List<TreeNode<E>> getChilds() {
		List<TreeNode<E>> result = new ArrayList<TreeNode<E>>();
		if(child != null) {
			return getChild().getSiblings(true);
		}
		return result;
	}
	
	public TreeNode<E> addChild(E data) {
		TreeNode<E> child = getChild();
		if(child == null) {
			TreeNode<E> ret = new TreeNode<E>(data);
			setChild(ret);
			return ret;
		}
		while(true) {
			if(child.getSibling() == null) {
				TreeNode<E> ret = new TreeNode<E>(data);
				child.setSibling(ret);
				return ret;
			}
			child = child.getSibling();
		}
	}
	
	public TreeNode<E> deleteChild(E data) {
		TreeNode<E> tmp = null;
		tmp = getChild();
		if(tmp == null) {
			return null;
		}
		if(tmp.getData() == data) {
			setChild(tmp.getSibling());
			return tmp;
		}
		TreeNode<E> pre = null;
		while(true) {
			pre = tmp;
			tmp = tmp.getSibling();
			if(tmp == null)
				return null;
			if(tmp.getData() == data) {
				pre.setSibling(tmp.getSibling());
				return tmp;
			}
		}		
	}

	public TreeNode<E> addSibling(E data) {
		if(parent != null) {
			return parent.addChild(data);
		} else {
			TreeNode<E> tmp = this.getSibling();
			while(true) {
				if(tmp == null) {
					TreeNode<E> ret = new TreeNode<E>(data);
					setSibling(ret);
					return ret;
				}
				tmp = tmp.getSibling();
			}
		}
	}

	public List<TreeNode<E>> getSiblings(boolean includeCurrent) {
		List<TreeNode<E>> result = new ArrayList<TreeNode<E>>();
		if(includeCurrent) {
			result.add(this);
		}
		TreeNode<E> sibling = getSibling();
		while(sibling != null) {
			result.add(sibling);
			sibling = sibling.getSibling();
		}
		return result;
	}
	
	public List<TreeNode<E>> getLeafs() {
		List<TreeNode<E>> result = new ArrayList<TreeNode<E>>();
		if(child != null) {
			result.addAll(child.getLeafs());
		} else {
			result.add(this);
		}
		if(sibling != null) {
			result.addAll(sibling.getLeafs());
		}
		return result;
	}

	public TreeNode<E> getChild() {
		return child;
	}
	
	public TreeNode<E> getSibling() {
		return sibling;
	}

	
	void setParent(TreeNode<E> parent) {
		this.parent = parent;
	}

	void setChild(TreeNode<E> child) {
		child.parent = this;
		child.depth = depth + 1;
		this.child = child;
	}

	void setSibling(TreeNode<E> sibling) {
		sibling.parent = this.parent;
		sibling.depth = depth;
		this.sibling = sibling;
	}
	
}
