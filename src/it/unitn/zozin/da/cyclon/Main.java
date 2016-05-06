package it.unitn.zozin.da.cyclon;

import it.unitn.zozin.da.cyclon.ControlActor.Configuration;
import it.unitn.zozin.da.cyclon.ControlActor.Configuration.Topology;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

public class Main {

	static Timeout SIM_MAX_TIME = Timeout.apply(FiniteDuration.create(20, TimeUnit.MINUTES));

	public static void main(String args[]) {
		ActorSystem s = ActorSystem.create();
		s.actorOf(Props.create(GraphActor.class), "graph");
		ActorRef control = s.actorOf(Props.create(ControlActor.class), "control");

		Configuration config = new Configuration();
		config.BOOT_TOPOLOGY = Topology.STAR;
		config.NODES = 10000;
		config.ROUNDS = 100;
		config.CYCLON_CACHE_SIZE = 10;
		config.CYCLON_SHUFFLE_LENGTH = 2;

		PatternsCS.ask(control, config, SIM_MAX_TIME).thenAccept((report) -> {
			System.out.println("FINAL REPORT: " + report);
			s.guardian().tell(PoisonPill.getInstance(), null);
		});
	}
}