// A Java Program to more formally test the ideas in my Non-Blocking-Hash-Map

import java.util.*;
import java.util.concurrent.*;

class NBHM_Tester {
  // Set of States for an individual State Machine.
  // Each State is really a pair of memory words.
  // The first word is only 0, K, X; the 2nd word is only 0, A/a, B/b, _ or x.
  enum S {                      // States
    BAD      (0),
    MT       (1),		// No Key, No Value
    X0       (2),		// Key is X'd out (slot is dead, nothing to copy)
    K0       (3),		// Key only, Value is NULL 
    KA       (4),		// Key/Value-A  pair
    Ka       (5),		// Key/Value-A' pair
    KB       (6),		// Key/Value-B  pair
    Kb       (7),		// Key/Value-B' pair
    K_       (8),		// Key/Tombstone - deleted
    KX       (9);		// Key/X pair - copied

    // A field to let me cheapo map to integers
    final int _idx;
    S(int idx) { _idx=idx; }
    static final int MAX = values().length;

    // --- compute_reached ---------------------------------------------------
    // Used to test sanity of the allowed-transitions
    private void compute_reached(boolean [] reached) {
      if( reached[_idx] ) return; // Already reached this state
      reached[_idx] = true;	  // First time reached this state
      S[] T = _allowed_transitions; // Short handy name
      // Visit all transitions...
      for( int i=0; i<T.length; i+= 2 )
	if( T[i] == this ) // If see a transition starting from this state
	  T[i+1].compute_reached(reached); // compute reaching from here
    }

    public static S [] _prime = {
      BAD,BAD,BAD,BAD,
      Ka,KA,Kb,KB,
      BAD,BAD
    };
    public S prime() { return _prime[_idx]; }
  };
  
  // List of allowed-transitions as S-pairs
  public static final S[] _allowed_transitions = {
    S.BAD, S.MT,                // Bogus starting transition
    S.MT, S.X0,			// Empty -> dead_slot
    S.MT, S.K0,			// Empty -> Key insertion

    S.K0, S.KA,			// Key   -> Key/A  pair
    S.K0, S.Ka,			// Key   -> Key/A' pair
    S.K0, S.KB,			// Key   -> Key/B  pair
    S.K0, S.Kb,			// Key   -> Key/B' pair
    S.K0, S.K_,			// Key   -> deleted

    S.KA, S.KB,			// Key/A -> Key/B
    S.KA, S.K_,			// Key/A -> deleted

    S.KB, S.KA,			// Key/B -> Key/A
    S.KB, S.K_,			// Key/B -> deleted

    S.K_, S.KA,			// deleted -> Key/A
    S.K_, S.KB,			// deleted -> Key/B

    S.Ka, S.KA,			// Key/A' -> Key/A (strip prime)
    S.Ka, S.Kb,			// Key/A' -> Key/B'
    S.Ka, S.K0,			// Key/A' -> Key alone (same as deleted-prime)
    S.Ka, S.KB,			// Key/A' -> Key/B (last write overrides copy)
    S.Ka, S.K_,			// Key/A' -> Key delete

    S.Kb, S.KB,			// Key/B' -> Key/B (strip prime)
    S.Kb, S.Ka,			// Key/B' -> Key/A'
    S.Kb, S.K0,			// Key/B' -> Key alone (same as deleted-prime)
    S.Kb, S.KA,			// Key/B' -> Key/A (last write overrides copy)
    S.Kb, S.K_,			// Key/B' -> Key delete

    S.K0, S.KX,			// Key   -> copied
    S.KA, S.KX,			// Key/A -> copied
    S.KB, S.KX,			// Key/B -> copied
    S.K_, S.KX,			// deleted -> copied

    null
  };
  // power-of-2 larger than _allowed_transitions.length
  private static final int LOG2TRAN = 6;
  private static final int MAXTRAN = 1<<LOG2TRAN;

  private static final int[][] fill_state_machine() {
    int [][] sm = new int[S.MAX][S.MAX];
    S[] T = _allowed_transitions; // Short handy name
    // Visit all transitions...
    for( int i=2; i<T.length-1; i+= 2 )
      sm[T[i+0]._idx][T[i+1]._idx] = i;
    return sm;
  }
  // Array of allowed transitions
  public static final int[][] _state_machine = fill_state_machine();
  // Is this transition allowed as part of the state-machine?
  public static final int is_SM(S x,S y) { return _state_machine[x._idx][y._idx]; }


  // --- Thrd ----------------------------------------------------------------
  // Notion of an action performed by a single thread, such as 'put(K,A)' or
  // 'delete(K)' - always with respect to key K.  This action will turn into a
  // series of state-machine transitions (or perhaps a request to move to a
  // newer state machine) or 

  public static abstract class Thrd {
    final String _name;		// Nice thread name
    final int _tid;		// This thread index; invariant: _Thrds[_tid]==this
    final boolean _ordered[];	// This thread is ordered after what other threads?
    static int _tids;		// Max number of threads
    static final Thrd[] _thrds = new Thrd[10]; // Array of them
    // Thread that can begin at any time
    Thrd( String name ) { 
      _tid = _tids++; 
      _name = name; 
      _thrds[_tid] = this; 
      _ordered = null;          // shortcut for un-ordered
    }
    // Thread that must wait until thread t0 has seen 'at_goal'
    Thrd( String name, Thrd t0 ) { 
      _tid = _tids++; 
      _name = name; 
      _thrds[_tid] = this; 
      _ordered = new boolean[_thrds.length];
      _ordered[t0._tid] = true;
    }
    // Thread that must wait until threads t0 and t1 have seen 'at_goal'
    Thrd( String name, Thrd t0, Thrd t1 ) { 
      _tid = _tids++; 
      _name = name; 
      _thrds[_tid] = this; 
      _ordered = new boolean[_thrds.length];
      _ordered[t0._tid] = true;
      _ordered[t1._tid] = true;
    }

    //abstract History step(History h);
    //abstract boolean at_goal(History h);
    public String toString() { return _name; }
    // threads cannot start until prior-ordered-threads finish.
    // passed in an array of active Thrds (or NULL)
    public boolean can_start( Thrd[] thrds ) {
      if( _ordered == null ) return true;
      for( int i=0; i<thrds.length; i++ )
        if( thrds[i] != null && _ordered[thrds[i]._tid] )
          return false;
      return true;
    }

    abstract boolean at_goal(History h);
    abstract History step( History h );
  }

  // --- Thrd_A --------------------------------------------------------------
  // A Thrd class to do a 'put(A)'
  public static class Thrd_A extends Thrd {
    Thrd_A( String n ) { super(n); }
    Thrd_A( String n, Thrd t0 ) { super(n,t0); }
    Thrd_A( String n, Thrd t0, Thrd t1 ) { super(n,t0,t1); }
    History step( History h ) {
      return step_impl(h,false,h._old);
    }
    History step_impl( History h, boolean old_or_new, S x ) {
      int tran = 0;
      switch( x ) {
      case MT: tran = is_SM(x,S.K0); break;
      case K0: 
      case Ka:
      case KB: 
      case Kb: 
      case K_: tran = is_SM(x,S.KA); break;
      case X0:
      case KX: return step_impl(h,true,h._new); // try again in new table
      default: 
	assert !at_goal(h) : "why you asking for a step when at_goal?";
	throw new Error("Unimplemented "+x);
      }
      assert tran != 0 : "broken step function from "+x;
      return h.make(new Event( x, old_or_new, tran ),this);
    }

    boolean at_goal(History h) {
      return 
        (h._old == S.KA) ||	// Old is KA OR
        (h._new == S.KA &&	// And new is KA
         h.old_is_dead());      // Signaled to new table
    }
  }

  // --- Thrd_B --------------------------------------------------------------
  // A Thrd class to do a 'put(B)'
  public static class Thrd_B extends Thrd {
    Thrd_B( String n ) { super(n); }
    Thrd_B( String n, Thrd t0 ) { super(n,t0); }
    Thrd_B( String n, Thrd t0, Thrd t1 ) { super(n,t0,t1); }
    History step( History h ) {
      return step_impl(h,false,h._old);
    }
    History step_impl( History h, boolean old_or_new, S x ) {
      int tran = 0;
      switch( x ) {
      case MT: tran = is_SM(x,S.K0); break;
      case K0: 
      case KA: 
      case Ka: 
      case Kb:
      case K_: tran = is_SM(x,S.KB); break;
      case X0:
      case KX: return step_impl(h,true,h._new); // try again in new table
      default: 
	assert !at_goal(h) : "why you asking for a step when at_goal?";
	throw new Error("Unimplemented "+x);
      }
      assert tran != 0 : "broken step function from "+x;
      return h.make(new Event( x, old_or_new, tran ),this);
    }

    boolean at_goal(History h) {
      return 
        (h._old == S.KB) ||	// Old is KB OR
        (h._new == S.KB &&	// And new is KB
         h.old_is_dead());      // Signaled to new table
    }
  }

  // --- Thrd_del ------------------------------------------------------------
  // A Thrd class to do a 'delete()'
  public static class Thrd_del extends Thrd {
    Thrd_del( String n ) { super(n); }
    Thrd_del( String n, Thrd t0 ) { super(n,t0); }
    Thrd_del( String n, Thrd t0, Thrd t1 ) { super(n,t0,t1); }

    History step( History h ) {
      return step_impl(h,false,h._old);
    }
    History step_impl( History h, boolean old_or_new, S x ) {
      switch( x ) {
      case KA: 
      case Ka: 
      case KB: 
      case Kb: {
        int tran = is_SM(x,S.K_);
        if( tran == 0 ) System.out.println(" del="+tran+" now="+x);
        return h.make(new Event( x, old_or_new, is_SM(x,S.K_) ),this);
      }
      case X0:
      case KX: return step_impl(h,true,h._new); // try again in new table
      default: 
	assert !at_goal(h) : "why you asking for a step when at_goal?";
	throw new Error("Unimplemented "+x);
      }
    }

    boolean at_goal(History h) {
      return 
        ( h._old == S.MT || h._old == S.K0 || h._old == S.K_) || // Old is K0 or K_ OR
        ((h._new == S.MT || h._new == S.K0 || h._new == S.K_) && // new is K0 or K_ and
         h.old_is_dead());      // Signaled to new table
    }
  }

  // --- Thrd_copy -----------------------------------------------------------
  // A Thrd class to do copy from the old table to the new table
  public static class Thrd_copy extends Thrd {
    Thrd_copy( String n ) { super(n); }
    Thrd_copy( String n, Thrd t0 ) { super(n,t0); }
    Thrd_copy( String n, Thrd t0, Thrd t1 ) { super(n,t0,t1); }

    History step_impl( History h, boolean old_or_new, S sold, S snew ) {
      if( (old_or_new ? h._new : h._old) != sold )
        // CAS fails because memory has changed; no actual transition happens;
        // re-read memory & try again
        return h.append_copy_reader(this,old_or_new);
      return h.make(new Event(sold,old_or_new,is_SM(sold,snew)),this);
    }

    History step( History h ) {
      S c_old = h.last_read( this, false ); // Last read value from OLD table
      S c_new = h.last_read( this, true  ); // Last read value from NEW table

      switch( c_old ) {
      case MT:  return step_impl(h,false,c_old,S.X0);
      case K0:  return step_impl(h,false,c_old,S.KX);
      case KA:
        switch( c_new ) {
        case MT:  return step_impl(h,true ,c_new,S.K0);
        case K0:  return step_impl(h,true ,c_new,S.Ka);
        case KA:
        case KB:
        case Ka:  return step_impl(h,false,c_old,S.KX);
        case Kb:  return step_impl(h,true ,c_new,S.Ka);
        }
        break;
      case KB:
        switch( c_new ) {
        case MT:  return step_impl(h,true ,c_new,S.K0);
        case K0:  return step_impl(h,true ,c_new,S.Kb);
        case Ka:  return step_impl(h,true ,c_new,S.Kb);
        case KA:
        case KB:
        case Kb:  return step_impl(h,false,c_old,S.KX);
        }
        break;
      case X0:
      case KX:
        switch( c_new ) {
        case Ka:  return step_impl(h,true ,c_new,S.KA);
        case Kb:  return step_impl(h,true ,c_new,S.KB);
        case K0:  return step_impl(h,true ,c_new,S.K_);
        }
        break;
      case K_:
        switch( c_new ) {
        case Ka:
        case Kb:  return step_impl(h,true ,c_new,S.K0);
        case MT:  
        case K0:  return step_impl(h,false,c_old,S.KX);
        }
        break;
      }
      
      throw new Error("Unimplemented copy "+h+" for old "+c_old + " for new "+c_new); 
    }
    boolean at_goal(History h) { 
      S hold = h.last_read( this, false ); // Last read value from OLD table
      S hnew = h.last_read( this, true  ); // Last read value from NEW table
      return (hold==S.X0 || hold==S.KX) &&
	hnew != S.Ka && hnew != S.Kb && hnew != S.K0;
    }
  }

  // --- Event ---------------------------------------------------------------
  // An Event is a transition in either the old or new finite state machine,
  // or a coherent read by some copy thread
  static public final class Event {
    // This is a bit-field, the low bits hold the transitions, the high
    // bits hold the TIDs of copy-threads.
    private final int _tran;

    public final int hashCode() { return _tran; }
    public final boolean equals( Object x ) {
      if( !(x instanceof Event) ) return false;
      return ((Event)x)._tran == _tran;	// Bits-compare-equal
    }

    public final int tran() { return _tran & (MAXTRAN-1); }
    public Event( S begin, boolean old_or_new, int tran ) {
      // Sane transitions
      assert tran != 0 : "reserved for copy-read Events";
      assert (tran&1)==0 && _allowed_transitions[tran] == begin;
      if( old_or_new ) tran |= (1<<LOG2TRAN); // Mark as a new-transition
      _tran = tran;
      assert !is_copyread();
    }
    public boolean old_or_new() { return (_tran & (1<<LOG2TRAN)) != 0; }
    public boolean is_copyread( ) { return tran() == 0; }
    public Event( Thrd_copy copy, boolean old_or_new ) {
      int tran = 0;
      if( old_or_new ) tran |= (1<<LOG2TRAN); // Mark as a new-transition
      _tran = tran;
      assert is_copyread();
    }
  }


  // --- History--------------------------------------------------------------
  // A sequence of interesting Events: finite-state-machine transitions
  // (including which thread caused them) for both old and new FSM's and
  // coherent reads by COPY threads.

  // The event arrays are immutable and hashed for equivalence, and are used
  // to explore the state-sequence-space.  The sequence always starts from the
  // same starting point: both FSM's at state MT.  Two histories are the same
  // if the FSM transitions & reads occur in the same order (reguardless of
  // thread). 

  // All threads that can could have done a particular transition are recorded
  // for that Event, so that later I can piece together plausible thread
  // interleavings that lead to bad states.
  static public class History {
    final Event [] _events;	// What event
    final int [] _tids;		// Which threads got here
    final S _old;		// Cache of last old-FSM state
    final S _new;		// Cache of last new-FSM state
    boolean _complete;		// Set to true if there exists a path where all threads are done

    // helper: old FSM is dead
    boolean old_is_dead() { return _old == S.X0 || _old == S.KX; }

    // --- hash --------------------------------------------------------------
    private final static ConcurrentHashMap<History,History> hash = 
      new ConcurrentHashMap<History,History>();
    private int _hash;
    public final int hashCode() { return _hash; }
    // Two Historys are 'equals' if they have the same state sequences.
    public final boolean equals( Object x ) {
      if( !(x instanceof History) ) return false;
      History h = (History)x;
      if( _events.length != h._events.length ) return false;
      for( int i=0; i<_events.length; i++ )
        if( !_events[i].equals(h._events[i]) ) 
         return false;
      return true;
    }

    // --- canonical -----------------------------------------------------------
    // Return the canonical History here, using the hash table.
    // Allows Histories to be compared using pointer-equivalence.
    private History canonical( ) {
      if( _events.length > 0 ) {
	Event e = _events[_events.length-1];
	S end = _allowed_transitions[e.tran()+1];
	assert e.old_or_new() || (end != S.Ka && end != S.Kb) :
	"No Primes in old table: "+e.old_or_new()+" "+end;
      }
      History old = hash.putIfAbsent(this,this);
      if( old == null ) return this;
      // Combine thread-ids in the old History
      for( int i=0; i<_tids.length; i++ )
        old._tids[i] |= _tids[i];
      return old;
    }

    // --- History -----------------------------------------------------------
    // The initial empty history
    private History() {
      _events = new Event[0];
      _tids = new int[_events.length];
      _hash = 1;
      _old = S.MT;
      _new = S.MT;
    }
    public static History make() { return new History().canonical(); }

    // --- History -----------------------------------------------------------
    // Extend an existing history
    private History(History h, Event e, Thrd t) {
      assert e.is_copyread() || (e.old_or_new() ? h._new : h._old) == _allowed_transitions[e.tran()];
      int idx = h._events.length;
      //assert (idx == 0) || !e.is_copyread() || !h._events[idx-1].is_copyread() : "no 2 copyreads in a row "+h;

      _events = new Event[idx+1];
      System.arraycopy(h._events,0,_events,0,idx);
      _tids   = new int  [idx+1];
      System.arraycopy(h._tids  ,0,_tids  ,0,idx);
      _events[idx] = e;
      _tids  [idx] = t != null ? (1<<t._tid) : 0;
      _hash = h.hashCode() + e.hashCode();
      if( e.is_copyread() ) {
        _old = h._old;  _new = h._new;
      } else {
        if( e.old_or_new() ) { _new = _allowed_transitions[e.tran()+1]; _old=h._old; }
        else                 { _old = _allowed_transitions[e.tran()+1]; _new=h._new; }
      }
    }
    public History make(Event e, Thrd t) { 
      return new History(this,e,t).canonical(); 
    }
    // Does this history already exist?
    public History check(Event e, Thrd t) { return hash.get(new History(this,e,t)); }
    public History add_at_goal( Thrd t ) {
      if( _tids.length > 0 ) _tids[_tids.length-1] |= (1<<t._tid);
      return this;
    }

    public History append_copy_reader( Thrd_copy t, boolean old_or_new ) {
      Event e = _events[_events.length-1];
      if( e.is_copyread() &&    // Allow more than 1 thread to read at same time
          e.old_or_new() == old_or_new &&
          _tids.length > 0 )
        return add_at_goal(t);  // BREAKS COPYREAD HASHING?
      Event ec = new Event( t, old_or_new ); // Add a coherent-copy-read
      return new History(this,ec,t).canonical(); 
    }

    // --- last_read ---------------------------------------------------------
    // Last value read by this thread for the given FSM.  Only interesting for
    // making changes in the OTHER FSM.
    public S last_read( Thrd_copy copy, boolean old_or_new ) {
      for( int i=_events.length-1; i>=0; i-- ) {
        Event e = _events[i];
        if( e.old_or_new() == old_or_new ) { // Matching FSM
          boolean was = (_tids[i]&(1<<copy._tid)) != 0;
          if( e.is_copyread( ) && // We just did a copy-thread-read?
              was ) {           // of the correct thread?
            for( i=i-1; i>=0 ; i-- ) { // Find last update to this FSM
              e = _events[i];   
              if( e.old_or_new() == old_or_new && !e.is_copyread() ) {
                return _allowed_transitions[e.tran()+1];
              }
            }
            return S.MT;
          }            
          // Or a normal copy-thread update
          if( was )
            return _allowed_transitions[e.tran()+1];
        }
      }
      return S.MT;              // Not ever read before
    }

    // --- toString ----------------------------------------------------------
    // Pretty print
    public String toString() {
      S s_old = S.MT;
      S s_new = S.MT;
      StringBuffer buf = new StringBuffer();
      buf.append("(").append(s_old).append("/").append(s_new);
      for( int i=0; i<_events.length; i++ ) {
        buf.append(" --");
	// Print all threads involved here
        long tids = _tids[i];
        int t=0;
        boolean first = true;
        while( tids != 0 ) {
          if( (tids & (1<<t)) != 0 ) {
            tids -= (1<<t);
            if( !first ) buf.append(",");
            buf.append(Thrd._thrds[t]);
            first = false;
          }
          t++;
        }
	// Update the states based on the transition
	Event e = _events[i];
        S s = e.old_or_new() ? s_new : s_old;
        if( e.is_copyread() ) {
          buf.append("--> [").append(e.old_or_new()?"new ":"old ").append(s).append("]");
        } else {
          assert _allowed_transitions[e.tran()] == s;
          s = _allowed_transitions[e.tran()+1]; // New State
          if( e.old_or_new() ) s_new = s; else s_old = s;
          // Print the New World Order
          buf.append("--> ").append(s_old).append("/").append(s_new);
        }
      }
      buf.append(")");
      assert s_old == _old;
      assert s_new == _new;
      return buf.toString();
    }

    // --- printAll ----------------------------------------------------------
    // Pretty print ALL histories
    public static void printAll     () { 
      for( History h : hash.keySet() ) {
        System.out.println(h);
      }
    }

    public static void printComplete() { 
      for( History h : hash.keySet() ) {
        if( h._complete )
          System.out.println(h);
      }
    }

    // --- witness -----------------------------------------------------------
    // Report back all the visible 'get' values possible
    public static void printWitness() { 
      for( History h : hash.keySet() ) {
        if( h._complete )
          System.out.println(h.witness()+" "+h);
      }
    }

    /**
     * Describe <code>witness</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String witness() {
      S s_old = S.MT;
      S s_new = S.MT;
      S s_last = S.MT;
      StringBuffer buf = new StringBuffer();
      buf.append("{");
      for( int i=0; i<_events.length; i++ ) {
	Event e = _events[i];
        if( !e.is_copyread() ) {
          // Update the states based on the transition
          S s = e.old_or_new() ? s_new : s_old;
          s = _allowed_transitions[e.tran()+1]; // New State
          if( e.old_or_new() ) s_new = s; else s_old = s;
          // Read from old first, or new if old is dead
          s = (s_old == S.X0 || s_old == S.KX) ? s_new : s_old;
          // 'flatten' answers
          if( s == S.K0 || s == S.K_ ) s = S.MT;
          if( s == S.Ka ) s = S.KA;
          if( s == S.Kb ) s = S.KB;
          if( s != s_last ) {
            buf.append(s).append(" ");
            s_last = s;
          }
        }
      }
      buf.append("}");
      return buf.toString();
    }

    // --- search ------------------------------------------------------------
    // Search the state space for running N threads, each stepping to some
    // goal.  Try stepping each thread 1 step from the current state.
    public void search(final Thrd[] q) {
      // Search 1 step for each thread
      boolean all_threads_done = true;
      for( int i=0; i<q.length; i++ ) {
        Thrd T = q[i];
        if( T == null )         // Can we advance this thread anymore?
          continue;             // Nope
        all_threads_done = false;
        if( !T.can_start(q) )   // Can start this thread yet?
          continue;             // Nope
	// Do One Step, unless at-goal already
	History h = T.at_goal(this) ? this.add_at_goal(T) : T.step(this);
	// Found a new state; must explore
	Thrd[] r = q.clone();	  // Clone old thread list
	if( T.at_goal(h) )	  // Hit goal?
	  r[i] = null;		  // No more advance on this thread
	h.search(r);		  // Search On!
      }
      if( all_threads_done )    // All threads done at this history
        _complete = true;       // so mark it
    }
  }

  // --- main ----------------------------------------------------------------
  public static void main( String[] args ) {
    if( MAXTRAN <= _allowed_transitions.length )
      throw new Error("Write some log2 function for MAXTRAN or bump its size");

    // Validate that all states reachable
    boolean [] reached = new boolean[S.MAX];
    S.MT.compute_reached(reached);
    for( S s : S.values() )
      if( !reached[s._idx] && s != S.BAD )
        throw new Error("State "+s+" not reachable from any transition");


    Thrd a0 = new Thrd_A("a0");
    //Thrd a1 = new Thrd_A("a1");
    Thrd b0 = new Thrd_B("b0");
    //Thrd d0 = new Thrd_del("d0");
    //Thrd d1 = new Thrd_del("d1");
    Thrd c0 = new Thrd_copy("c0");
    Thrd c1 = new Thrd_copy("c1");
    Thrd[] thrds = {a0,b0,c0,c1};
    History h = History.make();
    h.search(thrds);

    // need to check for no-dropped-last-write and also no-flip-flops

    // need to remove extra paths with redundant copy-reads, such as when c0
    // does a read, another thread updates, c0 fails a CAS and does another
    // read.

    //History.printAll();
    //History.printComplete();
    History.printWitness();
  }
}

