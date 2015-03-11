package AD3;
/**
 * This is a class where we use to connect the Arc class in C++
 * @author lingpenk
 *
 */
public class Arc {
	//  The class code in C++.
	//	class Arc {
	//		 public:
	//		  Arc(int h, int m) : h_(h), m_(m) {}
	//		  ~Arc() {}
	//
	//		  int head() { return h_; }
	//		  int modifier() { return m_; }
	//
	//		 private:
	//		  int h_;
	//		  int m_;
	//		};
	
	public Arc(int h, int m){
		h_ = h;
		m_ = m;
	}
	
	public int head() { return h_;}
	public int modifier() { return m_;}
	
	private int h_;
	private int m_;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + h_;
		result = prime * result + m_;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Arc other = (Arc) obj;
		if (h_ != other.h_)
			return false;
		if (m_ != other.m_)
			return false;
		return true;
	}
	
	public String toString(){
		return "" + h_ + " " + m_;
		
	}
	
}
