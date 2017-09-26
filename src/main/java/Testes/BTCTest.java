/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Testes;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;

/**
 *
 * @author admin_000
 */
public class BTCTest {

    /**
     * @param args the command line arguments
     * @throws org.bitcoinj.store.BlockStoreException
     */
    public static void main(String[] args) throws BlockStoreException {
        NetworkParameters networkParameters = RegTestParams.get();
        File f = new File("wallet");
        Wallet wallet;
        try {
            wallet = Wallet.loadFromFile(f, null);
        } catch (UnreadableWalletException ex) {
            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            wallet = new Wallet(networkParameters);
        }

        wallet.autosaveToFile(f, 0, TimeUnit.MINUTES, null);
        BlockStore blockStore = new MemoryBlockStore(networkParameters);
        BlockChain blockChain = new BlockChain(networkParameters, blockStore);
        blockChain.addWallet(wallet);

        PeerGroup peerGroup = new PeerGroup(networkParameters, blockChain);
        peerGroup.setUserAgent("Sample App", "0.14");
        peerGroup.addWallet(wallet);
        wallet.addWatchedAddress(Address.fromBase58(networkParameters, "n4YthQUE2GSX2EUGx76832QDHq8drfNMYi"));
        InetSocketAddress address = new InetSocketAddress("192.168.1.220", 18444);

        peerGroup.setUseLocalhostPeerWhenPossible(false);

        peerGroup.start();

        //peerGroup.connectToLocalHost();
        peerGroup.connectTo(address);
        System.out.println("Started");
        peerGroup.downloadBlockChain();
        System.out.println("BlockChain downloaded");

        System.out.println(peerGroup.getConnectedPeers());
        System.out.println("Completo " + blockChain.getBestChainHeight());
        System.out.println("Send Address: " + wallet.currentReceiveAddress());

        System.out.println(wallet.getWatchedOutputs(true));
        System.out.println("Endereços assistidos: " + wallet.getWatchedAddresses());
        System.out.println("Endereços: " + wallet.getIssuedReceiveAddresses());
        System.out.println("Moedas atuais " + wallet.getBalance(Wallet.BalanceType.AVAILABLE).toFriendlyString());
        System.out.println(wallet.getTransactions(false).iterator().next());

        Scanner sc = new Scanner(System.in);
        System.out.println("Escolha sua opção:\n 1 - Enviar");
        System.out.print("\\>");

        switch (sc.nextInt()) {
            case 1:
                System.out.println("Quanto quer enviar?");
                int qtd = sc.nextInt();
                System.out.println("Qual endereço?");
                String sendAddress = sc.next();
                send(qtd, sendAddress, wallet, networkParameters);
                break;
            default:
                System.out.println("Sem opções, saindo do programa");
        }

        try {
            wallet.saveToFile(f);
        } catch (IOException ex) {
            Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Carteira não pode ser salva.");
        }
        peerGroup.stop();

    }

    private static void send(int qtd, String sendAddress, Wallet wallet, NetworkParameters networkParameters) {
        Transaction tx = new Transaction(networkParameters);
        tx.addOutput(Coin.valueOf(qtd, 0), Address.fromBase58(networkParameters, sendAddress));
        {
            try {
                wallet.sendCoins(SendRequest.forTx(tx));

            } catch (InsufficientMoneyException ex) {
                Logger.getLogger(BTCTest.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Erro - Sem moedas suficientes para transação.");
            }

            final ListenableFuture<Coin> balanceFuture = wallet.getBalanceFuture(Coin.valueOf(qtd, 0), Wallet.BalanceType.AVAILABLE);
            FutureCallback<Coin> callback = new FutureCallback<Coin>() {
                public void onSuccess(Coin balance) {
                    System.out.println("Moedas enviadas com sucesso!");

                }

                public void onFailure(Throwable t) {
                    System.out.println("Moedas não puderam ser enviadas, por favor tentar novamente mais tarde.");
                }
            };
            Futures.addCallback(balanceFuture, callback);
            System.out.println("Moedas atuais " + wallet.getBalance(Wallet.BalanceType.AVAILABLE).toFriendlyString());

        }

    }
}
