package fr.xebia.ldi.fighter.actor

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.kafka.ProducerMessage
import breeze.stats.distributions.Gaussian
import fr.xebia.ldi.fighter.entity.ArenaEntity.ArenaEntity
import fr.xebia.ldi.fighter.entity.CharacterEntity.CharacterEntity
import fr.xebia.ldi.fighter.entity.FieldEntity.District
import fr.xebia.ldi.fighter.schema.{Arena, Player, Round}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.DateTime
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.rng.Seed

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


/**
  * Created by loicmdivad.
  */
case class Terminal(id: Int, location: ArenaEntity, publisher: ActorRef) extends Actor {

  val arena: Arena = Arena(location)
  val default: Gen.Parameters = Gen.Parameters.default.withSize(3000)
  val gaussians = Array(Gaussian(50, 35), Gaussian(25, 10), Gaussian(75, 10))

  val selector: Gen[Player] = for {
    dist <-  Gen.oneOf(gaussians)
    character <- arbitrary[CharacterEntity]
    combos <- Gen.chooseNum(0, 8)
    hits <- Gen.chooseNum(0, 3)
  } yield Player(character, score(dist), combos, hits)

  private def score(dist: Gaussian): Int = dist
    .map(Math.min(100, _))
    .map(Math.max(0, _))
    .draw().toInt

  override def receive = {
    case _ =>
      val dt = Gen.choose(0.1, 1.5).sample.get
      context.system.scheduler.scheduleOnce(dt seconds, self, play())
  }


  def play(): Unit = {
    val (winner, looser) = select()

    val round = Round(arena.id, id, winner, looser, District, DateTime.now().getMillis)

    val record: ProducerRecord[String, GenericRecord] =
      new ProducerRecord(s"ROUNDS", key, Round.roundFormat.to(round))

    val message = ProducerMessage.Message(record, NotUsed)
    publisher ! message
  }

  /**
    * Return a tuple of two Player
    * @return
    */
  def select(): (Player, Player) = (
    selector pureApply (default, Seed.random()),
    selector pureApply (default, Seed.random()) copy(life = 0)
  )

  def key: String = s"${arena.id}-$id-${System.currentTimeMillis()}"

}

case object Terminal {

  def actors(id: Int, location: ArenaEntity, publisher: ActorRef)(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props.apply(classOf[Terminal], id, location, publisher))

}
