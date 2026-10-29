package local.simple.streamguardobserver;
public final class DialogPolicyTest {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 public static void main(String[] args){
  DialogPolicy p=new DialogPolicy();check(p.allow(10,1000),"first dialog");p.consume(10,1000);
  check(!p.allow(10,8999),"same-dialog retry observes cooldown");
  check(p.allow(10,9000),"one bounded retry after cooldown");p.consume(10,9000);
  check(!p.allow(10,20000),"same visible dialog capped at two attempts");
  check(!p.allow(11,16999),"new dialog observes global cooldown");
  check(p.allow(11,17000),"new dialog after cooldown");p.consume(11,17000);
  check(!p.allow(12,100000),"rolling budget caps all attempts including retries");
  check(p.allow(12,121000),"rolling budget recovers");
  DialogPolicy q=new DialogPolicy();q.consume(11,1000);q.consume(11,9000);
  q.observe(false,10000);q.observe(true,11000);check(!q.allow(11,20000),"transient missing nodes do not reset attempts");
  q.observe(false,20000);q.observe(false,21500);check(q.allow(11,21500),"confirmed absence allows reused window");
  check(!p.allow(-1,300000),"invalid window never actionable");
  System.out.println("DialogPolicyTest passed");
 }
}
